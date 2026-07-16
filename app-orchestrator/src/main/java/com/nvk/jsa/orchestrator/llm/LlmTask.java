package com.nvk.jsa.orchestrator.llm;

/**
 * Task types routed to different models/providers (CLAUDE.md §3a).
 * EXTRACTION, EMBEDDING → Gemini (cheaper); GENERATION, AGENT → Claude (guardrail-critical).
 */
public enum LlmTask {
    EXTRACTION,
    EMBEDDING,
    RESEARCH_SUMMARY,
    GENERATION,
    AGENT
}
