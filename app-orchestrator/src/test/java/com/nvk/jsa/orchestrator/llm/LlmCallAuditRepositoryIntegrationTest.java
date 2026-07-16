package com.nvk.jsa.orchestrator.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.nvk.jsa.orchestrator.TestcontainersConfiguration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class LlmCallAuditRepositoryIntegrationTest {

    @Autowired
    private LlmCallAuditRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void insertPersistsProviderAndModelForAuditing() {
        UUID userId = UUID.randomUUID();
        repository.insert(new LlmCallAudit(UUID.randomUUID(), userId, LlmTask.EXTRACTION,
                "google-genai", "gemini-2.5-flash-lite", 120, 40, 350L, true, Instant.now()));

        Long count = jdbcClient
                .sql("SELECT COUNT(*) FROM llm_call_audit WHERE user_id = :userId AND provider = :provider")
                .param("userId", userId)
                .param("provider", "google-genai")
                .query(Long.class)
                .single();

        assertThat(count).isEqualTo(1L);
    }
}
