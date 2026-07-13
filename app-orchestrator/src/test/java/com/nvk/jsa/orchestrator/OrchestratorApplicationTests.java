package com.nvk.jsa.orchestrator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrchestratorApplicationTests {

    @Test
    void contextLoads() {
    }
}
