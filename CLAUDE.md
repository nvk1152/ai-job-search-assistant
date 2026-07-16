# CLAUDE.md — ai-job-search-assistant

> Operational guide for Claude Code. This file is loaded into every session. Keep it lean and current.
> The full product spec lives in `docs/PROJECT.md` — read it once for context, then rely on this file
> for day-to-day rules. If this file and the spec disagree, **this file wins**; flag the conflict to me.

---

## 1. What this is

ai-job-search-assistant is an **agentic job-application copilot**. A user pastes a job description and picks a base
résumé; a Claude-driven agent orchestrates MCP servers to: parse the JD, pull the user's *real*
experience, research the company from the web, and produce **drafts** — a tailored résumé, a cover
letter, a fit-gap analysis, and interview talking points. Applications are tracked on a Kanban board.
The agent host and guardrail-critical generation run on **Claude**; cheap, high-volume steps (extraction,
embeddings, company research) run on **Google Gemini**'s free-tier Developer API, selected by a
config-driven `ModelRouter` (§3a). Routing is a cost choice, never a safety one (§6).

**The whole value proposition is grounding/safety**: the agent may only *reframe* real experience
(never invent it), and company claims must be cited from search. Treat the guardrails in §6 as the
product, not as an afterthought.

**Current state:** greenfield. Start at **Phase 0** (§8). The build/run commands in §5 describe the
targets you will *create* in Phase 0 — they do not run yet. Do not assume any code exists until you've
read the tree.

---

## 2. Golden rules (read before writing any code)

1. **Understand, then architect, then code.** For any non-trivial task, restate the requirement,
   surface a short design (components, data flow, API/tool contracts, data model touchpoints, key
   choices with trade-offs), and **wait for my confirmation** before full implementation — unless I say
   "just code it."
2. **Never invent APIs.** Do not fabricate a Spring AI class, MCP annotation, Gradle DSL method, or
   config key. If unsure whether something exists in the pinned versions, say so and check the
   reference docs (§4) rather than guessing. Version-drift is the single biggest risk on this stack
   (see §4) — a confident wrong import costs more than a "let me verify."
3. **Server never trusts the client for identity or authorization.** `userId` and roles come from the
   validated app JWT / DB, never from request bodies, query params, or anything the model produced.
   This is a hard invariant (§6).
4. **Small, reviewable changes.** Work phase by phase (§8). When editing, show only changed
   files/sections with clear paths — not the whole codebase.
5. **No secrets in code.** Externalize all config; never hardcode API keys, DB URLs, or
   environment-specific values. Read secrets from env / Spring config, `.env` is git-ignored.
6. **Tests are part of "done."** Anything with logic ships with unit tests; the guardrail and
   tenant-isolation suites (§7) are mandatory, not optional.
7. **Push back when I'm wrong.** If a request has a flaw or a better approach exists, say so with
   reasoning before complying.
8. **Route models through `ModelRouter`; never hardcode model IDs.** Each step resolves its model from
   config by `LlmTask` (§3a). **Guardrail-critical generation — tailored résumé, cover letter, fit-gap,
   and the tool-calling agent loop — must stay on Claude**; cheap high-volume steps (extraction,
   embeddings, research) use Gemini's free tier. Moving a guardrail-critical task off Claude is a design
   error (§6).

---

## 3. Tech stack (pinned)

All versions verified current as of 2026-07-13. Confirm the latest **patch** before pinning in the BOM;
don't change **major/minor** without asking me.

| Layer            | Choice                                                                                 |
| ---------------- | -------------------------------------------------------------------------------------- |
| Language / build | **Java 25 (LTS)**, **Gradle 9.x** multi-module (Java 25 toolchain; daemon may run JDK 21+) |
| Framework        | **Spring Boot 4.1.x** (Spring Framework 7, Jakarta EE 11, first-class Java 25). **Do NOT use 3.5.x — OSS EOL 2026-06-30.** |
| AI               | **Spring AI 2.0.x** — `spring-ai-starter-model-anthropic` (official Anthropic Java SDK) **+** `spring-ai-starter-model-google-genai` (Gemini). Requires Spring Boot 4 as a hard dependency. |
| MCP              | `spring-ai-starter-mcp-client` (orchestrator) + `spring-ai-starter-mcp-server-webmvc` (servers). MCP Java SDK 2.0.0. Prefer **Streamable HTTP** transport (SSE is being phased out). |
| Model            | **Routed by `ModelRouter` (§3a).** Claude Sonnet — agent host + guardrail-critical generation; Claude Haiku — extraction fallback; Gemini 2.5 Flash-Lite — extraction; Gemini 2.5 Flash — research summaries; `gemini-embedding-001` — embeddings. **Do not hardcode model IDs** — resolve from config; confirm current IDs in each provider's console. |
| Web research     | web-search MCP server wrapping Brave Search / Tavily (API key via config); `company_brief` summaries composed by Gemini 2.5 Flash (§3a). Native web-search grounding is a fallback option. |
| Persistence      | **PostgreSQL + pgvector** (profile embeddings via `gemini-embedding-001`, JD matching, audit). Postgres **row-level security** for tenancy. Migrations via **Flyway**. |
| Doc parsing      | Apache **PDFBox / Tika** to extract text from uploaded résumés; the extraction model (Gemini 2.5 Flash-Lite, Haiku fallback) structures it into the schema. |
| Frontend         | **React + TypeScript + Vite**, Tailwind; Kanban via **dnd-kit**; Monaco/rich-text for doc editing. Separate build under `frontend/`. |
| Auth             | **Google OIDC only** → app-issued short-lived JWT. Roles carried as a JWT claim; feature access resolved **server-side per request** (§6a). |
| Ops              | Docker, Docker Compose (local Postgres), Helm chart, deploy to OpenShift. CI via GitHub Actions. |

---

## 3a. Model routing (Claude + Gemini) — verify starter coords against the 2.0 reference

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
- **Guardrail-critical stays on Claude** — `GENERATION` and `AGENT` never resolve to a Gemini model (§6).
- **Resilience:** extraction falls back Gemini → Haiku on error/rate-limit; retry-with-back-off both
  providers. Only the research server makes a model call among the MCP servers; the profile server is
  pure data.

---

## 4. Spring AI 2.0 / MCP gotchas — verify against the 2.0 reference docs

Spring AI 2.0 is a breaking rewrite. **Most pre-2026 tutorials and much of your prior training show
stale imports and dead APIs.** When you touch AI/MCP code, treat the official 2.0 reference as the
source of truth and assume snippets found elsewhere are wrong until proven otherwise.

Known breaking points (all verified for 2.0.x):

- **MCP annotations moved into core:** `@McpTool`, `@McpResource`, `@McpPrompt`,
  `@McpToolParam` live in **`org.springframework.ai.mcp.annotation`** — *not* the old
  `org.springaicommunity.mcp.*`. Code importing the community package won't compile.
- **Spring MCP transports moved into Spring AI:** they're under **`org.springframework.ai.mcp`**, no
  longer in the MCP Java SDK (`io.modelcontextprotocol.sdk`).
- **Jackson 3:** package is **`tools.jackson.*`**, not `com.fasterxml.jackson.*`. Use `JsonMapper`
  (via `JacksonUtils.getDefaultJsonMapper()` / the new `JsonHelper`) where you need direct JSON access.
- **Options classes are immutable, builder-only.** Setters were removed. Use
  `AnthropicChatOptions.builder().model(...).maxTokens(...).temperature(...).build()`. No
  `setTemperature(...)`.
- **Tool error contract:** throw **`ToolExecutionException`** with a model-readable message from an
  `@McpTool` method. A plain `RuntimeException` bypasses the error processor and can halt the agent
  loop.
- **MCP servers validate tool args against the JSON schema by default** (2.0). Make sure every tool's
  params carry accurate schemas; malformed/missing args now return a structured error to the client.
- **Anthropic module = thin adapter over `com.anthropic:anthropic-java`.** For anything the
  `AnthropicChatModel` abstraction doesn't expose (beta endpoints, files API), drop to the SDK client
  directly — don't reimplement it.
- Enable the MCP annotation scanner in config (`spring.ai.mcp.server.annotation-scanner.enabled`);
  a `@Component` with annotated methods is enough — no manual `ToolCallback` bean wiring.
- **Google GenAI starter has two modes** — free Gemini Developer API (api-key only) vs paid Vertex
  (`project-id`/`location`); mixing them yields confusing 400 auth errors. Multiple providers coexist via
  `spring.ai.model.chat` + qualified beans. See §3a.

Reference docs (fetch these rather than relying on memory):
`https://docs.spring.io/spring-ai/reference/` and the 2.0 upgrade notes.

---

## 5. Repo layout & commands

### Module layout (Gradle multi-module)

**Base Java package:** `com.nvk.jsa` — every module's sources live under it, one subpackage per module.

```
ai-job-search-assistant/
├── settings.gradle.kts
├── build.gradle.kts            // shared config, Java 25 toolchain
├── common/                     // com.nvk.jsa.common      — DTOs: Profile, Experience, JobDescription, Application
├── app-orchestrator/           // com.nvk.jsa.orchestrator — Spring Boot agent host + REST/SSE API + MCP clients
├── mcp-profile-server/         // com.nvk.jsa.mcp.profile  — MCP server: tenant-scoped, read-only profile access
├── mcp-research-server/        // com.nvk.jsa.mcp.research — MCP server: web search / company research
├── frontend/                   // React + TS (separate build)
└── docs/PROJECT.md             // full product spec
```

Keep Gradle module directory names (`app-orchestrator`, `mcp-profile-server`, …) as-is; only the Java
package base changed to `com.nvk.jsa`. Spring Boot main classes go at each module's package root so
component scanning stays scoped to that module (e.g. `com.nvk.jsa.orchestrator.OrchestratorApplication`).

### Commands (targets to create in Phase 0 — verify before assuming they run)

```bash
# Backend
./gradlew build                              # build + test all modules
./gradlew :app-orchestrator:bootRun          # run the orchestrator
./gradlew :mcp-profile-server:bootRun        # run an MCP server
./gradlew test                               # all tests
./gradlew :app-orchestrator:test --tests '*Guardrail*'   # guardrail suite

# Infra
docker compose up -d                         # local Postgres + pgvector
# Flyway migrations run on app startup (spring-boot Flyway integration)

# Frontend
cd frontend && npm install && npm run dev    # Vite dev server
cd frontend && npm run build && npm run test
```

If a command above doesn't exist yet, **create the wiring for it in Phase 0** rather than inventing
flags. Prefer the Gradle wrapper (`./gradlew`) over a system Gradle.

---

## 6. Guardrails — hard invariants (never violate; call these out in PRs)

These are the product's differentiators. Treat any change that weakens one as a design error and stop.

1. **No fabrication.** The agent may reorder, reword, and emphasize experience that exists in the
   user's profile. It must **never** invent a skill, employer, title, or date. Constrain via system
   prompt **and** retrieval — don't rely on the prompt alone.
2. **Grounded company claims.** Anything asserted about a company must come from `mcp-research-server`
   results and carry a source. The agent does not state company facts from memory.
3. **Backend owns identity.** `userId` is injected server-side into every MCP profile call; the model
   never supplies it. The profile server can only ever return the authenticated user's data. Every
   query is filtered by `user_id` and backed by Postgres RLS.
4. **Human-in-the-loop output.** All artifacts are **drafts** the user edits/approves. Nothing is
   auto-sent. Outputs are copy/download only (this deliberately avoids email-send OAuth scopes).
5. **Audit everything.** Every MCP tool call is written to `tool_call_audit`
   (user, server, tool, args hash, duration, ok). Flag flips are audited too.
6. **Server-side feature enforcement.** A hidden frontend button is UX, not security. Every gated
   endpoint and every gated MCP tool registration re-checks the flag server-side (§6a). Roles come from
   the DB-backed identity, are stamped into the JWT at login, and are **never** accepted from client input.
7. **Guardrail-critical generation stays on Claude.** Model routing (§3a) is a cost optimization, not a
   safety lever. Extraction, embeddings, and research summaries may run on Gemini; the no-fabrication
   generation and the tool-calling agent loop run on Claude. Weakening this is a design error.

### 6a. Role-based feature flags (progressive rollout)

New capabilities ship **dark** behind a flag and are enabled per-role before (or instead of) going
global. Design points to honor:

- **Single source of truth:** one `FeatureGate` service, `isEnabled(featureKey, user)`, implementing
  the rule `enabled_globally || (roles ∩ enabled_roles ≠ ∅)`. This rule lives in exactly one place.
- **DB-backed** (`feature_flag` table) so rollout changes at runtime without redeploy. Cache in-memory
  with a short TTL (~30s).
- **Three enforcement points, all server-side:**
    1. REST — `@RequiresFeature("web_research")` + interceptor → `403` with a machine-readable
       `feature_disabled` body.
    2. Agent loop — gated MCP tools are simply **not registered** into the ChatClient tool set for that
       request. The model can't call what it can't see.
    3. Frontend — `GET /api/me/entitlements` returns the resolved set for cosmetic show/hide/badge only.
- **Fail closed.** Flag lookup error ⇒ feature off. Unknown/missing key ⇒ disabled. Deleting a flag row
  disables the feature. Forgetting to seed a flag can never accidentally expose one.
- Roles: `USER` (default), `BETA`, `PREMIUM`, `ADMIN`. Stored as `text[]` on `app_user`.

---

## 7. Testing (required, not optional)

- **Guardrail tests — the most important suite.** Feed a profile *without* skill X and assert the
  tailored résumé never claims X. Add cases for invented employers/dates. Point interviewers here.
- **Tenant isolation.** The profile MCP server returns only the authenticated user's data even when the
  model is prompted to fetch another user's; RLS blocks cross-tenant reads.
- **Role/flag tests.** `USER` hitting a `BETA`-gated endpoint gets `403`; same request succeeds after
  `enabled_roles` gains their role (no redeploy); gated MCP tools are absent from the tool set for
  unentitled users; unknown/missing flag ⇒ off (fail-closed); non-`ADMIN` cannot modify flags;
  `/api/me/entitlements` matches actual server enforcement.
- **Unit** — JD parsing, fit-gap scoring, profile→JD matching (JUnit 5 / JUnit 6 on this stack).
- **Integration** — Testcontainers (Postgres + pgvector); full loop JD-paste → drafts.
- **Frontend** — Kanban interactions; one Playwright happy-path e2e.

Grounding assertions should be deterministic where possible (assert on structure/citations, not on
exact model prose).

---

## 8. Phase plan — build one demoable slice at a time

Deliver and verify phase by phase. Don't jump ahead without finishing the "Done when" for the current
phase. Full detail is in `docs/PROJECT.md` §6.

- **Phase 0 — Foundations.** Gradle multi-module skeleton, Java 25 toolchain, `common` DTOs, base
  orchestrator + one empty MCP server, health endpoints, Postgres (Compose) + Flyway, CI.
  *Done when:* `./gradlew build` green, services start, CI passes.
- **Phase 1 — Profile domain + résumé import.** Profile CRUD + React editor; wire **both** model
  providers + the `ModelRouter` (§3a); upload résumé → PDFBox/Tika text → **first model call** structures
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

---

## 9. Conventions

- **Java:** base package **`com.nvk.jsa`** (one subpackage per module — see §5). Clean,
  production-quality — meaningful names, small focused functions, SOLID where it earns
  its keep, no dead code. Proper error handling and logging; never swallow exceptions silently. Prefer
  records + immutability. Constructor injection over field injection.
- **API:** REST for CRUD, SSE for the agent's reasoning timeline. Return machine-readable error bodies
  (e.g. `feature_disabled`) with correct status codes.
- **DB:** every migration is a Flyway script, forward-only; every user-owned table filtered by
  `user_id` + RLS. No raw string SQL concatenation — parameterize.
- **Frontend:** TypeScript strict mode, functional components + hooks, no `any` without a comment
  justifying it.
- **Config:** everything environment-specific via Spring config / env vars. `.env` git-ignored. Model
  IDs and the `LlmTask`→model map are config, never hardcoded; both provider API keys (Anthropic +
  Gemini) come from env, never committed.
- **Commits/PRs:** describe the "why," call out any guardrail-adjacent change explicitly.

---

## 10. When in doubt

- If a decision materially affects the design → **list questions first**, don't silently guess.
- If it's a minor gap → state your assumption inline (e.g. "assuming Flyway since it's in the stack")
  and proceed, flagged so I can correct it.
- If your knowledge of a library/API/version might be stale → **say so and check the docs**. Honesty
  beats a confident guess on this stack.
