_Referenced from [CLAUDE.md](../CLAUDE.md) — model routing rules (Claude + Gemini)._

# Model routing (Claude + Gemini) — verify starter coords against the 2.0 reference

Two providers, one router. Cheap high-volume work → Gemini free tier; guardrail-critical work → Claude.

| `LlmTask` | Model | Notes |
| --------- | ----- | ----- |
| `EXTRACTION` | Gemini 2.5 Flash-Lite (→ Haiku fallback) | résumé/JD parsing to JSON |
| `EMBEDDING` | `gemini-embedding-001` | Anthropic has no embeddings API |
| `RESEARCH_SUMMARY` | Gemini 2.5 Flash | `company_brief` |
| `GENERATION` | **Claude Sonnet** | fit-gap, tailored résumé, cover letter (guardrail-critical) |
| `AGENT` | **Claude Sonnet** | tool-calling loop |

- **Billing (demo) = free Gemini Developer API, api-key only.** Set **only**
  `spring.ai.google.genai.api-key`. Setting `project-id`/`location` silently switches to **paid Vertex** —
  don't, unless you mean to (that same switch is the later production upgrade). Pin 2.5+ IDs; Gemini
  1.x/2.0 IDs are shut down.
- **`ModelRouter` is the single place model choice lives:** `LlmTask` → qualified `ChatClient` bean.
  Multiple providers coexist via `spring.ai.model.chat` + qualified beans. **Never hardcode model IDs**
  (config only). Log every routed call to `llm_call_audit` (task, provider, model, tokens).
- **Guardrail-critical stays on Claude** — `GENERATION` and `AGENT` never resolve to a Gemini model
  (see [GUARDRAILS_AND_FEATURE_FLAGS.md](GUARDRAILS_AND_FEATURE_FLAGS.md)).
- **Resilience:** extraction falls back Gemini → Haiku on error/rate-limit; retry-with-back-off both
  providers. Only the research server makes a model call among the MCP servers; the profile server is
  pure data.
