package com.nvk.jsa.orchestrator.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;

class ModelRouterTest {

    private final AnthropicChatModel anthropicChatModel = mock(AnthropicChatModel.class);
    private final GoogleGenAiChatModel googleGenAiChatModel = mock(GoogleGenAiChatModel.class);
    private final LlmCallAuditService auditService = mock(LlmCallAuditService.class);

    private final ModelRoutingProperties routing = new ModelRoutingProperties(
            new ModelRoutingProperties.ExtractionRoute(
                    "google-genai", "gemini-2.5-flash-lite", "anthropic", "claude-haiku-4-5-20251001"),
            new ModelRoutingProperties.Route("google-genai", "gemini-embedding-001"),
            new ModelRoutingProperties.Route("google-genai", "gemini-2.5-flash"),
            new ModelRoutingProperties.Route("anthropic", "claude-sonnet-5"),
            new ModelRoutingProperties.Route("anthropic", "claude-sonnet-5"));

    private final ModelRouter router = new ModelRouter(anthropicChatModel, googleGenAiChatModel, routing, auditService);

    @Test
    void extractionUsesGeminiWhenItSucceeds() {
        when(googleGenAiChatModel.call(any(Prompt.class))).thenReturn(chatResponse("{}"));

        String result = router.generate(LlmTask.EXTRACTION, UUID.randomUUID(), "system", "user");

        assertThat(result).isEqualTo("{}");
        verify(auditService).record(any(), eq(LlmTask.EXTRACTION), eq("google-genai"), eq("gemini-2.5-flash-lite"),
                any(), any(), anyLong(), eq(true));
    }

    @Test
    void extractionFallsBackToClaudeHaikuWhenGeminiFails() {
        when(googleGenAiChatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("rate limited"));
        when(anthropicChatModel.call(any(Prompt.class))).thenReturn(chatResponse("{}"));

        String result = router.generate(LlmTask.EXTRACTION, UUID.randomUUID(), "system", "user");

        assertThat(result).isEqualTo("{}");
        verify(auditService).record(any(), eq(LlmTask.EXTRACTION), eq("google-genai"), eq("gemini-2.5-flash-lite"),
                any(), any(), anyLong(), eq(false));
        verify(auditService).record(any(), eq(LlmTask.EXTRACTION), eq("anthropic"), eq("claude-haiku-4-5-20251001"),
                any(), any(), anyLong(), eq(true));
    }

    @Test
    void generationAlwaysResolvesToClaude() {
        when(anthropicChatModel.call(any(Prompt.class))).thenReturn(chatResponse("draft"));

        String result = router.generate(LlmTask.GENERATION, UUID.randomUUID(), "system", "user");

        assertThat(result).isEqualTo("draft");
        verify(anthropicChatModel, times(1)).call(any(Prompt.class));
        verify(googleGenAiChatModel, times(0)).call(any(Prompt.class));
    }

    @Test
    void embeddingIsRejectedAsNotAChatTask() {
        assertThrows(IllegalArgumentException.class,
                () -> router.generate(LlmTask.EMBEDDING, UUID.randomUUID(), "system", "user"));
    }

    private ChatResponse chatResponse(String text) {
        Generation generation = new Generation(new AssistantMessage(text));
        Usage usage = new Usage() {
            @Override
            public Integer getPromptTokens() {
                return 10;
            }

            @Override
            public Integer getCompletionTokens() {
                return 5;
            }

            @Override
            public Object getNativeUsage() {
                return null;
            }
        };
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        return new ChatResponse(List.of(generation), metadata);
    }
}
