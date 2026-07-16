package com.nvk.jsa.orchestrator.llm;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit trail for every LLM call: task, provider/model, token counts, duration, success/failure.
 * Written to database for cost tracking and compliance (CLAUDE.md §6).
 */
public record LlmCallAudit(
        UUID id,
        UUID userId,
        LlmTask llmTask,
        String provider,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        long durationMs,
        boolean ok,
        Instant createdAt) {
}
