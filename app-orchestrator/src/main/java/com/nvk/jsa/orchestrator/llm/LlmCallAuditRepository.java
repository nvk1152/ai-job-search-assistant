package com.nvk.jsa.orchestrator.llm;

import java.sql.Timestamp;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class LlmCallAuditRepository {

    private final JdbcClient jdbcClient;

    public LlmCallAuditRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void insert(LlmCallAudit audit) {
        jdbcClient.sql("""
                        INSERT INTO llm_call_audit
                            (id, user_id, llm_task, provider, model, input_tokens, output_tokens, duration_ms, ok, created_at)
                        VALUES (:id, :userId, :llmTask, :provider, :model, :inputTokens, :outputTokens, :durationMs, :ok, :createdAt)
                        """)
                .param("id", audit.id())
                .param("userId", audit.userId())
                .param("llmTask", audit.llmTask().name())
                .param("provider", audit.provider())
                .param("model", audit.model())
                .param("inputTokens", audit.inputTokens())
                .param("outputTokens", audit.outputTokens())
                .param("durationMs", audit.durationMs())
                .param("ok", audit.ok())
                .param("createdAt", Timestamp.from(audit.createdAt()))
                .update();
    }
}
