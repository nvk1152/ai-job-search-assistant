# ai-job-search-assistant — AI Job Application Copilot

> **Context (2026-07-13):** Formerly "CareerPilot." App renamed to **ai-job-search-assistant**; Java
> base package is **`com.nvk.jsa`**. Stack targets **Spring Boot 4.1.x** (3.5.x hit OSS EOL 2026-06-30)
> and **Spring AI 2.0.x** GA (MCP annotations in core, Jackson 3, official Anthropic Java SDK). A
> **dual-provider model layer** (§2a) routes each step to Claude or Google Gemini by capability and
> cost: guardrail-critical generation stays on Claude; cheap, high-volume work (extraction, embeddings,
> company research) runs on Gemini's **free-tier Developer API** to keep the demo at near-zero cost.
> Auth is **Google OIDC only**; a role-based feature-flag design rolls features out per-role instead of
> to all users. This is the full product spec — the operational rules Claude Code follows live in the
> root `CLAUDE.md`; if the two disagree, `CLAUDE.md` wins.

> Paste a job description + pick your base résumé, and an AI agent generates a tailored résumé,
> drafts a cover letter, and builds an interview-readiness brief with grounded company details.
> Track each application on a Kanban board. **All data is user-provided or freely searchable —
> nothing external can block your demo.**

**Product scope now**

- JD in → tailored résumé + cover letter drafts
- Company research summary + interview talking points (with sources)
- Fit-gap analysis against your real experience
- Application tracking workflow (Saved → Applied → Interview → Offer/Rejected)

**Future expansion**

- Search jobs from multiple sources and unify them into one pipeline view
- Assisted apply flow first, optional auto-apply mode later with strict user approval rules

---

## 1. Why this project (positioning)

The most universally legible AI project you can show: any interviewer instantly understands the value,
and it's literally the tool you'd use to land the job it's meant to win. It's self-contained (user
brings their résumé + JDs; company facts come from web search), so there are no restricted OAuth scopes
or regulated APIs to gate you.

**What it demonstrates**

- **Java 25 / Spring Boot** — multi-module orchestrator + MCP servers.
- **Agentic AI (Claude + Gemini)** — multi-step reasoning: parse JD → match against profile → research
  company → generate tailored artifacts.
- **MCP** — a web-research server and a tenant-scoped profile server, cleanly separated.
- **Cost-aware multi-model routing** — a provider-agnostic `ModelRouter` (§2a) sends each step to the
  cheapest capable model: Gemini's free tier for extraction/embeddings/research, Claude for the
  guardrail-critical generation. Real, defensible cost engineering — a strong interview talking point.
- **Grounding / anti-hallucination** — the agent may only _reframe_ real experience, never invent it;
  company claims are cited from search. This is a strong, mature talking point.
- **React + TypeScript** — résumé editor, JD intake, generated-doc previews, application Kanban.

**Elevator résumé bullet (target end state)**

> Built an agentic job-application copilot (Java 25 / Spring Boot / Spring AI, Claude + Gemini) that
> tailors résumés and drafts cover letters by orchestrating MCP servers for tenant-scoped profile access
> and web research; enforced a no-fabrication grounding guardrail so the model reframes only real
> experience and cites company facts from search; routed a cost-aware multi-model layer that keeps
> guardrail-critical generation on Claude while running high-volume extraction/embeddings/research on
> Gemini's free tier; React/TypeScript UI with a résumé editor and application-tracking Kanban;
> multi-tenant with per-user isolation and audit logging.

**Design emphasis:** keep CRUD deliberately light — the depth (and the interview story) sits in the
**agent + MCP + guardrail** layers, not in yet another entity-CRUD app.

---

## 2. Tech stack

Versions verified current as of 2026-07-13. Confirm the latest **patch** before pinning in the BOM;
don't move **major/minor** without a deliberate decision.

| Layer            | Choice                                                                                                                                              |
| ---------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Language / build | **Java 25 (LTS)**, **Gradle 9.x** multi-module (Java 25 toolchain; daemon may run on JDK 21+)                                                       |
| Framework        | **Spring Boot 4.x** (4.1.x as of mid-2026) — Spring Framework 7, Jakarta EE 11, first-class Java 25 support. _Do not start on 3.5.x: it reached OSS EOL June 30, 2026._ |
| AI               | **Spring AI 2.0.x** — `spring-ai-starter-model-anthropic` (official Anthropic Java SDK) **+** `spring-ai-starter-model-google-genai` (Gemini; free Developer API via api-key, or Vertex later — §2a). MCP annotations (`@McpTool`, `@McpResource`, `@McpPrompt`) are in core. _2.0 renamed MCP packages and moved to Jackson 3 — most pre-2026 tutorials show stale imports; follow the 2.0 reference docs (see §5b)._ |
| Model            | **Routed across two providers by a `ModelRouter` (§2a).** Claude Sonnet — agent host + guardrail-critical generation (fit-gap, tailored résumé, cover letter). Gemini 2.5 Flash-Lite — extraction (résumé/JD), with Claude Haiku as fallback. Gemini 2.5 Flash — company-research summarization. `gemini-embedding-001` — embeddings. **Do not hardcode model IDs** — resolve from config; confirm current IDs in each provider's console. |
| MCP              | `spring-ai-starter-mcp-client` (orchestrator) + `spring-ai-starter-mcp-server-webmvc` (servers). In Spring AI 2.0 the Spring MCP transports live in `org.springframework.ai`, no longer in the MCP Java SDK. Prefer **Streamable HTTP** transport (SSE is being phased out). |
| Web research     | a web-search MCP server (wrap Brave Search / Tavily API, key via config); `company_brief` summaries are composed by Gemini 2.5 Flash (§2a). Claude's/Gemini's native web-search grounding is a fallback option |
| Persistence      | **PostgreSQL** + **pgvector** (profile embeddings, JD matching, audit); Postgres **row-level security**; migrations via **Flyway**                  |
| Doc parsing      | Apache PDFBox / Tika to extract text from an uploaded résumé; the **extraction model** (Gemini 2.5 Flash-Lite, Haiku fallback — §2a) structures it into the schema |
| Frontend         | **React + TypeScript + Vite**, Tailwind; Kanban (dnd-kit), Monaco/rich-text for docs                                                                |
| Auth             | **Google OIDC** → app-issued JWT (roles carried as a JWT claim; feature access resolved server-side per request — see §5a).                          |
| Ops              | Docker, Helm chart, deploy to OpenShift; CI via GitHub Actions                                                                                     |

---

## 2a. Model routing (Claude + Gemini, by capability × cost)

Two providers, one router. The point is cost engineering that a reviewer can immediately follow — the
cheap, high-volume, low-risk work runs on Gemini's free tier; the guardrail-critical generation stays on
Claude. This is a deliberate design choice, not a safety compromise (see §5).

| Step | Model | Why |
| ---- | ----- | --- |
| Résumé + JD extraction (→ structured JSON) | **Gemini 2.5 Flash-Lite** (Haiku fallback) | Cheapest capable model; free-tier eligible; strong at structured extraction. |
| Profile / JD embeddings (pgvector) | **`gemini-embedding-001`** | Anthropic has **no embeddings API**, so this must come from elsewhere anyway; Gemini's is cheap and free-tier eligible. |
| Company-research summary (`company_brief`) | **Gemini 2.5 Flash** | Cheap; adequate for grounded summarization from search snippets. |
| Fit-gap analysis + tailored résumé + cover letter | **Claude Sonnet** | Guardrail-critical (no-fabrication, grounded reframing, tone). Keep on the stronger model. |
| Agent host / MCP tool-calling loop | **Claude Sonnet** | Strong instruction-following + tool use; the loop that enforces the guardrails. |

**Billing mode (demo): Gemini Developer API free tier — API key only.** Use the
`spring-ai-starter-model-google-genai` starter and set **only** `spring.ai.google.genai.api-key`.
Setting `project-id`/`location` silently switches the client to paid **Vertex AI** — do not set them for
the demo. The same starter/code path flips to Vertex later (production on OpenShift) by adding those two
properties, so this is a one-config-line upgrade, not a rewrite. Free-tier caveats: Flash / Flash-Lite
have daily request + per-minute token caps (fine for a demo); Pro models became paid-only in Apr 2026,
but this design uses no Pro model. Confirm current free-tier limits before relying on them.

**The `ModelRouter` abstraction.** Model choice lives in exactly one place: an `LlmTask` enum
(`EXTRACTION`, `EMBEDDING`, `RESEARCH_SUMMARY`, `GENERATION`, `AGENT`) → a qualified `ChatClient`/model
bean. Spring AI 2.0 supports multiple providers side by side (`spring.ai.model.chat` + qualified beans);
`ModelRouter.clientFor(task)` returns the right client. Model IDs and the task→model map are **config,
never hardcoded**. Every routed call is logged (`llm_call_audit`, §4) with task, provider, model, and
token counts, which is what makes the cost story auditable rather than aspirational.

**Fallback + resilience.** Extraction falls back from Gemini Flash-Lite to Claude Haiku on error /
rate-limit; both providers get retry-with-back-off (§7). MCP servers do **no** model calls except the
research server's `company_brief` summary — the profile server is pure data access, so routing only ever
touches the orchestrator and the research server.

**Verify before pinning.** The `google-genai` starter's exact artifact coordinates shifted in the Spring
AI 2.0 renaming — confirm against the 2.0 reference (§5b) rather than trusting older snippets.

---

## 3. Architecture

```
                         ┌─────────────────────────────┐
   React + TS UI ───────▶│  Orchestrator (Spring Boot)  │
   (JD intake, résumé    │  - REST + SSE API            │
    editor, Kanban,      │  - Auth / tenancy / audit    │
    doc previews)        │  - Spring AI + ModelRouter    │
                         │    (Claude host + Gemini)     │
                         │  - MCP *clients*              │
                         └──────┬────────────┬──────────┘
                  MCP (HTTP)    │            │
             ┌─────────────────┘            └─────────────────┐
             ▼                                                ▼
     ┌────────────────────┐                      ┌────────────────────┐
     │ mcp-profile-server │  (tenant-scoped,     │ mcp-research-server │
     │ read-only tools    │   read-only résumé   │ web search         │
     │ over user profile  │   facts — no invent) │ company lookup     │
     └────────────────────┘                      └────────────────────┘
             │                                                │
             └──────────────► PostgreSQL + pgvector ◄─────────┘
```

**Base Java package:** `com.nvk.jsa` — one subpackage per module. Put each Spring Boot main class at its
module's package root so component scanning stays scoped (e.g.
`com.nvk.jsa.orchestrator.OrchestratorApplication`).

**Gradle module layout**

```
ai-job-search-assistant/
├── settings.gradle.kts
├── build.gradle.kts                 // shared config, Java 25 toolchain
├── common/                          // com.nvk.jsa.common      — DTOs: Profile, Experience, JobDescription, Application
├── app-orchestrator/                // com.nvk.jsa.orchestrator — Spring Boot agent host + API
├── mcp-profile-server/              // com.nvk.jsa.mcp.profile  — MCP server: tenant-scoped read-only profile access
├── mcp-research-server/             // com.nvk.jsa.mcp.research — MCP server: web search / company research
├── frontend/                        // React + TS (separate build)
└── docs/PROJECT.md                  // this file
```

Keep the Gradle module directory names as-is; only the Java package base is `com.nvk.jsa`.

### Agent loop (multi-step, genuinely agentic)

1. User pastes a JD and selects a base résumé/profile.
2. Agent extracts the JD's must-haves, nice-to-haves, and keywords.
3. Agent calls **mcp-profile-server** to pull the user's real experience/skills (tenant-scoped).
4. Agent calls **mcp-research-server** to gather company facts (mission, product, recent news).
5. Agent produces, as **drafts**: a tailored résumé (reordered/reworded to surface matching real
   experience), a cover letter, a fit-gap analysis, and interview-readiness talking points.
6. User edits/approves. **Nothing is auto-sent** — outputs are copy/download only (avoids email-send
   scopes entirely and keeps a human in the loop).

**Model per step (via `ModelRouter`, §2a):** step 2 extraction → Gemini 2.5 Flash-Lite (Haiku fallback);
step 4 `company_brief` summary → Gemini 2.5 Flash; steps 3/5 and the tool-calling loop → Claude Sonnet.
The profile server itself makes no model calls.

### Tool catalog (illustrative)

| MCP server | Tool                       | Notes                                                            |
| ---------- | -------------------------- | ---------------------------------------------------------------- |
| profile    | `get_profile()`            | structured experience/skills/education for the current user only |
| profile    | `search_experience(query)` | semantic match of profile bullets to a JD requirement (pgvector) |
| research   | `web_search(query)`        | returns snippets + sources                                       |
| research   | `company_brief(name)`      | composed company summary with citations (summary by Gemini 2.5 Flash — §2a) |

> The profile server **only ever returns the authenticated user's data** — `userId` is injected by the
> orchestrator, never supplied by the model. This is the core tenant-isolation guarantee.

---

## 4. Data model (highlights)

- `app_user(id, google_sub UNIQUE, email UNIQUE, roles text[] DEFAULT '{USER}', created_at)`
  - Roles are a small closed set: `USER` (default), `BETA` (early access to new features),
    `PREMIUM` (plan-gated features), `ADMIN` (flag management + audit views). Kept as an array for
    simplicity; promote to a `user_role` join table only if roles grow attributes (expiry, grantor).
- `feature_flag(key PK, description, enabled_globally bool DEFAULT false, enabled_roles text[] DEFAULT '{}', updated_by, updated_at)`
  - A user can use feature `key` iff `enabled_globally` **or** `roles ∩ enabled_roles ≠ ∅`.
  - Examples: `web_research`, `interview_prep`, `auto_apply` (future — ships dark, enabled for `BETA` first).
- `profile(id, user_id, headline, summary)`
- `experience(id, profile_id, company, title, start, end, bullets[])`
- `skill(id, profile_id, name, level)`, `education(...)`
- `experience_embedding(experience_id, embedding vector)` — for JD→experience matching; vectors from
  `gemini-embedding-001` (§2a). The `vector` column's dimension must match the embedding model's output
  — verify the model's dimension before creating the column (and keep it fixed, or re-embed on change).
- `job_description(id, user_id, company, title, raw_text, parsed_requirements jsonb)`
- `application(id, user_id, jd_id, stage, notes, created_at, updated_at)` — Kanban stages
- `generated_doc(id, application_id, type, content, model, created_at)` — résumé/cover-letter versions;
  `model` records which provider/model produced each artifact (e.g. `claude-sonnet-...`).
- `tool_call_audit(id, user_id, mcp_server, tool, args_hash, duration_ms, ok, created_at)`
- `llm_call_audit(id, user_id, llm_task, provider, model, input_tokens, output_tokens, duration_ms, ok, created_at)`
  — one row per routed model call, so per-provider cost/latency is visible and the "cheap steps on
  Gemini" claim is auditable, not aspirational.
- Multi-tenancy: every query filtered by `user_id`; Postgres **row-level security**.

---

## 5. Guardrails (the differentiator — talk about this in interviews)

- **No fabrication.** System prompt + retrieval constrain the agent to reframe only facts present in the
  user's profile. It may reorder, reword, and emphasize — never invent a skill, employer, or date.
- **Grounded company claims.** Anything about the company must come from `mcp-research-server` results
  and carry a source; the agent doesn't assert company facts from memory.
- **Guardrail-critical generation stays on the stronger model.** Model routing (§2a) is a cost
  optimization, not a safety lever: extraction, embeddings, and research summaries may use cheaper Gemini
  models, but the no-fabrication generation (fit-gap, tailored résumé, cover letter) and the tool-calling
  agent loop run on Claude Sonnet. A router change that moves a guardrail-critical step to a weaker model
  is a design error.
- **Backend owns identity.** `userId` is injected server-side; the profile server can't be tricked into
  returning another user's data.
- **Human-in-the-loop output.** All artifacts are drafts the user edits/approves; no automated sending.
- **Audit log** of every tool call for traceability.
- **Server-side feature enforcement.** The frontend hiding a button is UX, not security: every gated
  endpoint and every gated MCP tool registration re-checks the flag server-side. Roles come from the
  DB-backed identity, are stamped into the app JWT at login, and are never accepted from client input.

### 5a. Role-based feature flags (progressive rollout)

New capabilities ship **dark** behind a flag and are enabled per-role before (or instead of) going
global — e.g., `auto_apply` goes `ADMIN` → `BETA` → `PREMIUM`, and may never be global.

- **Storage:** the `feature_flag` table (§4). DB-backed (not config-only) so a rollout can change at
  runtime without redeploy — an `ADMIN` flips it from a small admin screen (or SQL, day one).
- **Evaluation:** one `FeatureGate` service — `isEnabled(featureKey, user)` — is the *single* place
  the `enabled_globally || roles ∩ enabled_roles` rule lives. Cache flags in-memory with a short TTL
  (~30s): near-zero DB load, changes propagate in seconds, and stale-by-30s is acceptable for flags.
- **Enforcement points (all server-side):**
  1. REST — a `@RequiresFeature("web_research")` annotation + interceptor returning `403` with a
     machine-readable `feature_disabled` error body.
  2. Agent loop — gated MCP tools are simply **not registered** into the ChatClient tool set for that
     request; the model can't call what it can't see (cleaner than letting it call and fail).
  3. Frontend — `GET /api/me/entitlements` returns the resolved feature set once at load; UI shows,
     hides, or badges ("beta") accordingly. Purely cosmetic; the backend remains the authority.
- **Fail closed.** If flag lookup errors, the feature is off. Deleting a flag row disables the feature
  (unknown key ⇒ disabled) — so forgetting to seed a flag can never accidentally expose a feature.
- **Auditability:** `updated_by`/`updated_at` on the flag row; flag flips are themselves logged.

### 5b. Spring AI 2.0 / MCP gotchas (verify against the 2.0 reference docs)

Spring AI 2.0 is a breaking rewrite; assume snippets found elsewhere are stale until proven otherwise.
Reference: `https://docs.spring.io/spring-ai/reference/` + the 2.0 upgrade notes.

- MCP annotations (`@McpTool`, `@McpResource`, `@McpPrompt`, `@McpToolParam`) live in
  **`org.springframework.ai.mcp.annotation`** — not the old `org.springaicommunity.mcp.*`.
- Spring MCP transports moved into **`org.springframework.ai.mcp`** (out of the MCP Java SDK).
- **Jackson 3:** package is **`tools.jackson.*`** (not `com.fasterxml.jackson.*`); use `JsonMapper`.
- Model/MCP **options classes are immutable, builder-only** — setters removed. Use
  `AnthropicChatOptions.builder()...build()`.
- From an `@McpTool` method throw **`ToolExecutionException`** (model-readable message), never a plain
  `RuntimeException` — the latter can halt the agent loop.
- MCP servers **validate tool args against the JSON schema by default** in 2.0; keep tool param schemas
  accurate.
- The Anthropic module is a thin adapter over `com.anthropic:anthropic-java`; drop to the SDK client for
  anything the abstraction doesn't expose.
- **Google GenAI starter has two modes.** `spring-ai-starter-model-google-genai` uses the free Gemini
  Developer API when **only** `spring.ai.google.genai.api-key` is set; setting `project-id`/`location`
  silently switches to paid Vertex. Set only the api-key for the demo (§2a).
- **Multiple providers coexist** via `spring.ai.model.chat` + qualified `ChatModel`/`ChatClient` beans —
  that's how the `ModelRouter` (§2a) holds both the Anthropic and Google clients at once.
- **Gemini 1.x / 2.0 model IDs are shut down** (2.0-flash/-lite ended 2026-06-01). Target 2.5+
  (`gemini-2.5-flash-lite`, `gemini-2.5-flash`) and `gemini-embedding-001`; resolve exact IDs from config.

---

## 6. Phase-wise plan

Each phase is independently demoable and adds one clear capability.

### Phase 0 — Foundations

- Gradle multi-module skeleton; Java 25 toolchain; `common` module (Profile/JD/Application DTOs under
  `com.nvk.jsa.common`).
- Base Spring Boot orchestrator + one empty MCP server; health endpoints.
- PostgreSQL (Docker Compose) + Flyway; CI (GitHub Actions).
- **Done when:** `./gradlew build` green; services start; CI passes.

### Phase 1 — Profile domain + résumé import

- CRUD for structured profile (experiences, skills, education); React profile editor.
- Wire **both** model providers and stand up the `ModelRouter` (§2a) here — this is the first model
  call. Upload a résumé (PDF/DOCX) → extract text (PDFBox/Tika) → the **extraction model structures it**
  into the schema for user review (Gemini 2.5 Flash-Lite primary, Claude Haiku fallback; no MCP yet).
- **Done when:** a user can import a résumé and see/edit a structured profile; `llm_call_audit` records
  the call's provider/model.

### Phase 2 — JD intake + parsing

- Paste a JD → agent extracts must-haves, nice-to-haves, keywords into `parsed_requirements`
  (extraction routed to Gemini 2.5 Flash-Lite via `ModelRouter`, Haiku fallback).
- React JD view showing the parsed requirements.
- **Done when:** pasting a JD yields a clean structured requirement list.

### Phase 3 — Profile MCP server + fit-gap matching

- `mcp-profile-server`: `get_profile`, `search_experience` (tenant-scoped, read-only, pgvector;
  embeddings via `gemini-embedding-001` — §2a).
- Wire orchestrator MCP client; the agent pulls profile via MCP to produce a **fit-gap analysis**
  (which requirements are strongly/weakly/not covered by real experience).
- **Done when:** JD + profile → an accurate, grounded fit-gap report.

### Phase 4 — Research MCP server + tailored artifacts

- `mcp-research-server`: `web_search`, `company_brief` (with sources; summary composed by Gemini 2.5
  Flash — §2a).
- Agent composes JD + profile + research into: **tailored résumé draft**, **cover letter**, **talking
  points** — generation runs on Claude Sonnet, enforcing the no-fabrication guardrail and citing company
  facts.
- React: side-by-side original vs. tailored; SSE reasoning timeline.
- **Done when:** one action produces all three grounded, editable drafts.

### Phase 5 — Application tracker (Kanban)

- Stages: Saved → Applied → Interview → Offer → Rejected; drag-and-drop; per-application notes and
  generated-doc versions.
- Tracker exposed so the agent can answer "what's pending?" / "summarize my pipeline."
- **Done when:** applications move across the board and their generated docs are retained.

### Phase 6 — Auth, tenancy, roles, audit

- Google OIDC → app JWT; `userId` injected everywhere; Postgres RLS.
- Roles (`USER`/`BETA`/`PREMIUM`/`ADMIN`) stored on `app_user`, stamped into the JWT at login,
  re-resolved from DB on role-sensitive operations (JWTs are short-lived so revocation lag is bounded).
- **Feature-flag rollout system (§5a):** `feature_flag` table + `FeatureGate` service +
  `@RequiresFeature` interceptor + per-request MCP tool registration + `/api/me/entitlements`.
  First real use: gate `web_research` so it can be enabled for `BETA` before everyone.
- Minimal admin surface: list flags, toggle `enabled_globally`, edit `enabled_roles` (ADMIN only).
- Persist and surface `tool_call_audit`.
- **Done when:** two users are fully isolated; a feature can be turned on for `BETA` users only and a
  `USER` gets a clean 403 (API) and no button (UI) for it; every tool call is logged.

### Phase 7 — Hardening, ops, demo

- Tests (see §7); structured logging + metrics; rate limiting; graceful LLM error/back-off handling.
- Dockerize; Helm chart; deploy to OpenShift.
- README with architecture diagram + the demo script below.
- **Done when:** deployed and the demo runs end-to-end.

### Phase 8 — Job discovery + assisted/auto apply (future)

- Add a `mcp-jobs-server` (`com.nvk.jsa.mcp.jobs`) for job discovery and normalization (title, company,
  location, salary, source URL, posted date).
- Implement job-listing ingestion from public/free sources and de-duplicate similar roles.
- Add an **assisted apply** flow first: prefill from profile, generate responses, user reviews and
  confirms every submission.
- Add optional **auto-apply mode** behind explicit per-job consent, rule filters, and daily limits.
- Persist apply attempts and outcomes in audit logs.
- **Done when:** user can discover jobs in-app and run assisted applies; auto-apply remains opt-in with
  strong guardrails.

---

## 7. Testing strategy

- **Unit** — JD parsing, fit-gap scoring, profile→JD matching (JUnit 5 / JUnit 6 on this stack).
- **Guardrail tests** — feed a profile without skill X and assert the tailored résumé never claims X.
  This is the single most important test suite and a great thing to point to in interviews.
- **MCP server tests** — profile server returns only the authenticated user's data (isolation test);
  research server returns sources.
- **Role/flag tests** — `USER` calling a `BETA`-gated endpoint gets 403; the same request succeeds
  after the flag's `enabled_roles` gains `USER` (no redeploy); gated MCP tools are absent from the
  tool set for unentitled users (the model never sees them); unknown/missing flag key ⇒ feature off
  (fail-closed); non-`ADMIN` cannot modify flags; entitlements endpoint matches server enforcement.
- **Model-routing tests** — `ModelRouter` maps each `LlmTask` to the configured provider; every
  guardrail-critical task (generation, agent loop) resolves to Claude; extraction falls back to Claude
  Haiku when Gemini errors/rate-limits; a config that points a guardrail-critical task at a non-Claude
  model fails the test. The guardrail suite runs against whatever model is configured for generation.
- **Integration** — Testcontainers (Postgres); full loop from JD paste to generated drafts.
- **Frontend** — Kanban interactions; one Playwright happy-path e2e.

Prefer deterministic grounding assertions (assert on structure/citations, not on exact model prose).

---

## 8. Demo script

Have a sample profile pre-loaded and a real JD ready. Live flow: paste JD → agent parses requirements →
pulls your profile via MCP → runs company research → produces a tailored résumé + cover letter + fit-gap
with company facts cited → you tweak one line → save the application as a card → drag it from "Saved" to
"Applied." Highlight the guardrail: point at a requirement you _don't_ meet and show the agent flags it
as a gap rather than fabricating experience.

---

## 9. Stretch / future

- ATS keyword-coverage score + suggestions.
- Interview-prep mode: generate likely questions from the JD + your gaps.
- Export tailored résumé to a clean PDF/DOCX template.
- Optional email _draft_ generation (still copy-out, never auto-send).
- Unified job search inbox across multiple sources.
- Optional auto-apply agent with human approval policies and safety throttles — ships dark behind the
  `auto_apply` flag, enabled `ADMIN` → `BETA` first.
- Feature-flag upgrades if needed later: percentage rollouts, per-user overrides, or migrating to
  Togglz/Unleash once hand-rolled flags stop being enough.

---

## 10. First week, concretely

1. `gradle init` multi-module; Java 25 toolchain + Spring Boot 4.x and Spring AI 2.0.x BOMs; base
   package `com.nvk.jsa`. Add both model starters (`...-anthropic`, `...-google-genai`).
2. Postgres + Flyway; Profile schema + CRUD; minimal React profile editor.
3. Stand up the `ModelRouter` (§2a) and make the first model call: paste résumé text → structured
   profile JSON, routed to Gemini 2.5 Flash-Lite (Haiku fallback). Gemini uses the free Developer API
   (api-key only — no `project-id`/`location`).
4. Stand up `mcp-profile-server` with `get_profile` via `@McpTool`; wire the orchestrator MCP client.
   That's your end-to-end skeleton.