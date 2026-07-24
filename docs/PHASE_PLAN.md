_Referenced from [CLAUDE.md](../CLAUDE.md) — build one demoable slice at a time._

# Phase plan — build one demoable slice at a time

Deliver and verify phase by phase. Don't jump ahead without finishing the "Done when" for the current
phase. Full detail is in `docs/PROJECT.md` §6.

- **Phase 0 — Foundations.** Gradle multi-module skeleton, Java 25 toolchain, `common` DTOs, base
  orchestrator + one empty MCP server, health endpoints, Postgres (Compose) + Flyway, CI.
  *Done when:* `./gradlew build` green, services start, CI passes.
- **Phase 1 — Profile domain + résumé import.** Profile CRUD + React editor; wire **both** model
  providers + the `ModelRouter` (see [MODEL_ROUTING.md](MODEL_ROUTING.md)); upload résumé → PDFBox/Tika text → **first model call** structures
  it (extraction on Gemini 2.5 Flash-Lite, Haiku fallback; no MCP yet).
- **Phase 2 — JD intake + parsing** into `parsed_requirements`.
- **Phase 3 — `mcp-profile-server`** (`get_profile`, `search_experience`, tenant-scoped, pgvector) +
  fit-gap analysis via MCP.
- **Phase 4 — `mcp-research-server`** (`web_search`, `company_brief` with sources) + tailored résumé /
  cover letter / talking points, enforcing the no-fabrication guardrail.
- **Phase 5 — Kanban tracker** (Saved → Applied → Interview → Offer → Rejected), doc versions retained.
- **Phase 6 — Auth, tenancy, roles, audit.** Google OIDC → JWT, RLS, `FeatureGate` + `@RequiresFeature`
    + per-request MCP tool registration + `/api/me/entitlements`; persist `tool_call_audit`.
- **Phase 7 — Hardening/ops/demo.** Structured logging + metrics, rate limiting, LLM back-off, Docker,
  Helm, OpenShift, README with the demo script.
- **Phase 8 (future) — job discovery + assisted/auto apply.** Only after 0–7; auto-apply ships dark
  behind the `auto_apply` flag with explicit per-job consent.
