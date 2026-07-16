package com.nvk.jsa.orchestrator.llm;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Records all LLM calls to the audit table: task type, provider, model, tokens, latency, and
 * outcome. Critical for cost attribution and compliance (CLAUDE.md §6).
 */
@Service
public class LlmCallAuditService {

    private final LlmCallAuditRepository repository;

    public LlmCallAuditService(LlmCallAuditRepository repository) {
        this.repository = repository;
    }

    public void record(UUID userId, LlmTask task, String provider, String model,
            Integer inputTokens, Integer outputTokens, long durationMs, boolean ok) {
        repository.insert(new LlmCallAudit(
                UUID.randomUUID(), userId, task, provider, model,
                inputTokens, outputTokens, durationMs, ok, Instant.now()));
    }
}
