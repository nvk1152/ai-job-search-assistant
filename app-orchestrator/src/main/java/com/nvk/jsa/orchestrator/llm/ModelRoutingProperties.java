package com.nvk.jsa.orchestrator.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config-driven model routing map: LlmTask → (provider, model). Supports fallback routing
 * for extraction. Externalize to avoid hardcoding model IDs (CLAUDE.md §2, §3a).
 */
@ConfigurationProperties(prefix = "app.llm.routing")
public record ModelRoutingProperties(
        ExtractionRoute extraction,
        Route embedding,
        Route researchSummary,
        Route generation,
        Route agent) {

    public record Route(String provider, String model) {
    }

    public record ExtractionRoute(String primaryProvider, String primaryModel, String fallbackProvider, String fallbackModel) {
    }
}
