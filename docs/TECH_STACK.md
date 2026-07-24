_Referenced from [CLAUDE.md](../CLAUDE.md) — pinned tech stack._

# Tech stack (pinned)

All versions verified current as of 2026-07-13. Confirm the latest **patch** before pinning in the BOM;
don't change **major/minor** without asking me.

| Layer            | Choice                                                                                 |
| ---------------- | -------------------------------------------------------------------------------------- |
| Language / build | **Java 25 (LTS)**, **Gradle 9.x** multi-module (Java 25 toolchain; daemon may run JDK 21+) |
| Framework        | **Spring Boot 4.1.x** (Spring Framework 7, Jakarta EE 11, first-class Java 25). **Do NOT use 3.5.x — OSS EOL 2026-06-30.** |
| AI               | **Spring AI 2.0.x** — `spring-ai-starter-model-anthropic` (official Anthropic Java SDK) **+** `spring-ai-starter-model-google-genai` (Gemini). Requires Spring Boot 4 as a hard dependency. |
| MCP              | `spring-ai-starter-mcp-client` (orchestrator) + `spring-ai-starter-mcp-server-webmvc` (servers). MCP Java SDK 2.0.0. Prefer **Streamable HTTP** transport (SSE is being phased out). |
| Model            | **Routed by `ModelRouter` (see [MODEL_ROUTING.md](MODEL_ROUTING.md)).** Claude Sonnet — agent host + guardrail-critical generation; Claude Haiku — extraction fallback; Gemini 2.5 Flash-Lite — extraction; Gemini 2.5 Flash — research summaries; `gemini-embedding-001` — embeddings. **Do not hardcode model IDs** — resolve from config; confirm current IDs in each provider's console. |
| Web research     | web-search MCP server wrapping Brave Search / Tavily (API key via config); `company_brief` summaries composed by Gemini 2.5 Flash (see [MODEL_ROUTING.md](MODEL_ROUTING.md)). Native web-search grounding is a fallback option. |
| Persistence      | **PostgreSQL + pgvector** (profile embeddings via `gemini-embedding-001`, JD matching, audit). Postgres **row-level security** for tenancy. Migrations via **Flyway**. |
| Doc parsing      | Apache **PDFBox / Tika** to extract text from uploaded résumés; the extraction model (Gemini 2.5 Flash-Lite, Haiku fallback) structures it into the schema. |
| Frontend         | **React + TypeScript + Vite**, Tailwind; Kanban via **dnd-kit**; Monaco/rich-text for doc editing. Separate build under `frontend/`. |
| Auth             | **Google OIDC only** → app-issued short-lived JWT. Roles carried as a JWT claim; feature access resolved **server-side per request** (see [GUARDRAILS_AND_FEATURE_FLAGS.md](GUARDRAILS_AND_FEATURE_FLAGS.md)). |
| Ops              | Docker, Docker Compose (local Postgres), Helm chart, deploy to OpenShift. CI via GitHub Actions. |
