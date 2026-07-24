_Referenced from [CLAUDE.md](../CLAUDE.md) — hard invariants and role-based feature flags. This is the product's differentiator — read before touching generation, MCP profile/research code, or auth._

# Guardrails — hard invariants (never violate; call these out in PRs)

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
   endpoint and every gated MCP tool registration re-checks the flag server-side (see below). Roles come from
   the DB-backed identity, are stamped into the JWT at login, and are **never** accepted from client input.
7. **Guardrail-critical generation stays on Claude.** Model routing ([MODEL_ROUTING.md](MODEL_ROUTING.md)) is a cost optimization, not a
   safety lever. Extraction, embeddings, and research summaries may run on Gemini; the no-fabrication
   generation and the tool-calling agent loop run on Claude. Weakening this is a design error.

## Role-based feature flags (progressive rollout)

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
