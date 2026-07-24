_Referenced from [CLAUDE.md](../CLAUDE.md) — required test coverage, not optional._

# Testing (required, not optional)

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

See also [GUARDRAILS_AND_FEATURE_FLAGS.md](GUARDRAILS_AND_FEATURE_FLAGS.md) for what these tests are protecting.
