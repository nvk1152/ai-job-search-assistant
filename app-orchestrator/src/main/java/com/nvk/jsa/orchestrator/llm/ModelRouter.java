package com.nvk.jsa.orchestrator.llm;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Component;

/**
 * Single place LlmTask → provider/model resolution lives (CLAUDE.md §3a). Routes guardrail-critical
 * tasks (GENERATION, AGENT) to Claude; extraction and research to Gemini. Extraction auto-falls back
 * from Gemini to Claude Haiku on error. Every call is audited for cost and compliance.
 */
@Component
public class ModelRouter {

    private static final Logger log = LoggerFactory.getLogger(ModelRouter.class);

    private final AnthropicChatModel anthropicChatModel;
    private final GoogleGenAiChatModel googleGenAiChatModel;
    private final ModelRoutingProperties routing;
    private final LlmCallAuditService auditService;

    public ModelRouter(
            AnthropicChatModel anthropicChatModel,
            GoogleGenAiChatModel googleGenAiChatModel,
            ModelRoutingProperties routing,
            LlmCallAuditService auditService) {
        this.anthropicChatModel = anthropicChatModel;
        this.googleGenAiChatModel = googleGenAiChatModel;
        this.routing = routing;
        this.auditService = auditService;
    }

    public String generate(LlmTask task, UUID userId, String systemPrompt, String userPrompt) {
        if (task == LlmTask.EMBEDDING) {
            throw new IllegalArgumentException("EMBEDDING is not a chat-completion task");
        }
        if (task == LlmTask.EXTRACTION) {
            return generateExtraction(userId, systemPrompt, userPrompt);
        }
        ModelRoutingProperties.Route route = routeFor(task);
        return call(userId, task, route.provider(), route.model(), systemPrompt, userPrompt);
    }

    private String generateExtraction(UUID userId, String systemPrompt, String userPrompt) {
        ModelRoutingProperties.ExtractionRoute route = routing.extraction();
        try {
            return call(userId, LlmTask.EXTRACTION, route.primaryProvider(), route.primaryModel(), systemPrompt, userPrompt);
        } catch (RuntimeException primaryFailure) {
            log.warn("Extraction primary provider {} model {} failed; falling back to {} model {}",
                    route.primaryProvider(), route.primaryModel(),
                    route.fallbackProvider(), route.fallbackModel(), primaryFailure);
            return call(userId, LlmTask.EXTRACTION, route.fallbackProvider(), route.fallbackModel(), systemPrompt, userPrompt);
        }
    }

    private ModelRoutingProperties.Route routeFor(LlmTask task) {
        return switch (task) {
            case RESEARCH_SUMMARY -> routing.researchSummary();
            case GENERATION -> routing.generation();
            case AGENT -> routing.agent();
            case EXTRACTION, EMBEDDING -> throw new IllegalStateException("handled separately: " + task);
        };
    }

    private String call(UUID userId, LlmTask task, String provider, String model, String systemPrompt, String userPrompt) {
        long start = System.currentTimeMillis();
        try {
            ChatResponse response = switch (provider) {
                case "anthropic" -> anthropicChatModel.call(new Prompt(
                        List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                        AnthropicChatOptions.builder().model(model).build()));
                case "google-genai" -> googleGenAiChatModel.call(new Prompt(
                        List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                        GoogleGenAiChatOptions.builder().model(model).build()));
                default -> throw new IllegalStateException("Unknown LLM provider: " + provider);
            };
            String content = response.getResult().getOutput().getText();
            Integer inputTokens = response.getMetadata().getUsage() != null
                    ? response.getMetadata().getUsage().getPromptTokens() : null;
            Integer outputTokens = response.getMetadata().getUsage() != null
                    ? response.getMetadata().getUsage().getCompletionTokens() : null;
            auditService.record(userId, task, provider, model, inputTokens, outputTokens,
                    System.currentTimeMillis() - start, true);
            return content;
        } catch (RuntimeException e) {
            auditService.record(userId, task, provider, model, null, null,
                    System.currentTimeMillis() - start, false);
            throw e;
        }
    }
}
