_Referenced from [CLAUDE.md](../CLAUDE.md) — module layout and build/run commands._

# Repo layout & commands

## Module layout (Gradle multi-module)

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

## Commands (targets to create in Phase 0 — verify before assuming they run)

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
