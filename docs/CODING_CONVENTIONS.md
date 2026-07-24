_Referenced from [CLAUDE.md](../CLAUDE.md) — Java/API/DB/frontend/config/commit conventions._

# Conventions

- **Java:** base package **`com.nvk.jsa`** (one subpackage per module — see [REPO_LAYOUT_AND_COMMANDS.md](REPO_LAYOUT_AND_COMMANDS.md)). Clean,
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
