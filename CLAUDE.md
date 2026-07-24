# CLAUDE.md — ai-job-search-assistant

> Operational guide for Claude Code. This file is loaded into every session — kept lean on purpose.
> The full product spec lives in `docs/PROJECT.md` — read it once for context. Everything else that
> used to live here is split into the topic files below; pull one in when you're actually working in
> that area. If this file and any referenced doc disagree, **this file wins**; flag the conflict to me.

## Reference docs (pull in when relevant)

| File | Read it when... |
| ---- | ---------------- |
| `docs/PROJECT.md` | You need full product spec / context (read once). |
| `docs/TECH_STACK.md` | You're adding a dependency or unsure what version/library is pinned. |
| `docs/MODEL_ROUTING.md` | You're touching `ModelRouter`, adding an `LlmTask`, or calling Claude/Gemini. |
| `docs/SPRING_AI_MCP_GOTCHAS.md` | You're writing Spring AI or MCP code (annotations, transports, options, tool errors). |
| `docs/REPO_LAYOUT_AND_COMMANDS.md` | You need the module layout or a build/run/test command. |
| `docs/GUARDRAILS_AND_FEATURE_FLAGS.md` | You're touching generation, MCP profile/research servers, auth, or feature gating. |
| `docs/TESTING_REQUIREMENTS.md` | You're writing or reviewing tests, especially guardrail/tenant-isolation ones. |
| `docs/PHASE_PLAN.md` | You need to know what phase we're in / what's in scope next. |
| `docs/CODING_CONVENTIONS.md` | You need Java/API/DB/frontend/config/commit style rules. |

---

## 1. What this is

ai-job-search-assistant is an **agentic job-application copilot**. A user pastes a job description and picks a base
résumé; a Claude-driven agent orchestrates MCP servers to: parse the JD, pull the user's *real*
experience, research the company from the web, and produce **drafts** — a tailored résumé, a cover
letter, a fit-gap analysis, and interview talking points. Applications are tracked on a Kanban board.
The agent host and guardrail-critical generation run on **Claude**; cheap, high-volume steps (extraction,
embeddings, company research) run on **Google Gemini**'s free-tier Developer API, selected by a
config-driven `ModelRouter` (`docs/MODEL_ROUTING.md`). Routing is a cost choice, never a safety one
(`docs/GUARDRAILS_AND_FEATURE_FLAGS.md`).

**The whole value proposition is grounding/safety**: the agent may only *reframe* real experience
(never invent it), and company claims must be cited from search. Treat the guardrails in
`docs/GUARDRAILS_AND_FEATURE_FLAGS.md` as the product, not as an afterthought.

**Current state:** greenfield. Start at **Phase 0** (`docs/PHASE_PLAN.md`). The build/run commands in
`docs/REPO_LAYOUT_AND_COMMANDS.md` describe the targets you will *create* in Phase 0 — they do not run
yet. Do not assume any code exists until you've read the tree.

---

## 2. Golden rules (read before writing any code)

1. **Understand, then architect, then code.** For any non-trivial task, restate the requirement,
   surface a short design (components, data flow, API/tool contracts, data model touchpoints, key
   choices with trade-offs), and **wait for my confirmation** before full implementation — unless I say
   "just code it."
2. **Never invent APIs.** Do not fabricate a Spring AI class, MCP annotation, Gradle DSL method, or
   config key. If unsure whether something exists in the pinned versions, say so and check the
   reference docs rather than guessing. Version-drift is the single biggest risk on this stack
   (see `docs/SPRING_AI_MCP_GOTCHAS.md`) — a confident wrong import costs more than a "let me verify."
3. **Server never trusts the client for identity or authorization.** `userId` and roles come from the
   validated app JWT / DB, never from request bodies, query params, or anything the model produced.
   This is a hard invariant (`docs/GUARDRAILS_AND_FEATURE_FLAGS.md`).
4. **Small, reviewable changes.** Work phase by phase (`docs/PHASE_PLAN.md`). When editing, show only
   changed files/sections with clear paths — not the whole codebase.
5. **No secrets in code.** Externalize all config; never hardcode API keys, DB URLs, or
   environment-specific values. Read secrets from env / Spring config, `.env` is git-ignored.
6. **Tests are part of "done."** Anything with logic ships with unit tests; the guardrail and
   tenant-isolation suites (`docs/TESTING_REQUIREMENTS.md`) are mandatory, not optional.
7. **Push back when I'm wrong.** If a request has a flaw or a better approach exists, say so with
   reasoning before complying.
8. **Route models through `ModelRouter`; never hardcode model IDs.** Each step resolves its model from
   config by `LlmTask` (`docs/MODEL_ROUTING.md`). **Guardrail-critical generation — tailored résumé,
   cover letter, fit-gap, and the tool-calling agent loop — must stay on Claude**; cheap high-volume
   steps (extraction, embeddings, research) use Gemini's free tier. Moving a guardrail-critical task off
   Claude is a design error (`docs/GUARDRAILS_AND_FEATURE_FLAGS.md`).

---

## 3. When in doubt

- If a decision materially affects the design → **list questions first**, don't silently guess.
- If it's a minor gap → state your assumption inline (e.g. "assuming Flyway since it's in the stack")
  and proceed, flagged so I can correct it.
- If your knowledge of a library/API/version might be stale → **say so and check the docs**. Honesty
  beats a confident guess on this stack.
