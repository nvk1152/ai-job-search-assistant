# AI Job Search Assistant

[![Build Status](https://github.com/nvk1152/ai-job-search-assistant/actions/workflows/ci.yml/badge.svg)](https://github.com/nvk1152/ai-job-search-assistant/actions)
[![Phase](https://img.shields.io/badge/phase-1%20(Foundations)-blue)](#current-phase)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

**An agentic job-application copilot powered by Claude and grounded in your real experience.**

Paste a job description → AI analyzes fit → generates tailored résumé, cover letter, talking points. All outputs are **human-reviewed drafts** that only reframe your real experience—never fabricate.

- **🛡️ Guardrail-first:** No invented employers, titles, or skills. Sources cited for company claims.
- **🔐 Secure:** User identity server-owned; audit trail for all LLM calls.
- **⚡ Smart routing:** Expensive Claude for critical generation; cheap Gemini for extraction & research.
- **📱 React UI:** Clean profile editor with résumé import (PDF/DOCX).

**Status:** Phase 1 (Foundations) ✅ — profile management + résumé import live. Phase 2–7 in progress.

---

## Table of Contents

- [What is This?](#what-is-this)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Running Locally](#running-locally)
- [Project Structure](#project-structure)
- [API Reference](#api-reference)
- [Phase 1 Status](#phase-1-status)
- [Guardrails & Safety](#guardrails--safety)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

---

## What is This?

### The Problem
Applying for jobs is tedious: research company, rewrite résumé, craft cover letter, prepare talking points—all for each application. Most people give up after a few tailored applications and blast generic ones.

### The Solution
This project automates the **heavy lifting** without breaking trust:

1. **Profile:** Upload your résumé (PDF/DOCX) → AI extracts experiences, skills, education
2. **Job Description:** Paste a job posting
3. **Agent Loop:** Claude-powered agent orchestrates MCP servers to:
   - Match your profile against JD requirements
   - Research the company (grounded from web search)
   - Generate fit-gap analysis, tailored résumé, cover letter, interview talking points
4. **Review & Edit:** You review all drafts, edit, download, or save for later
5. **Track:** Kanban board tracks applications (Saved → Applied → Interview → Offer)

### Why This Matters
- **Grounded:** Company facts come from search results (cited). Your experience is never invented.
- **Safe:** You own all data; nothing auto-sends; everything is a draft you approve.
- **Cost-efficient:** Cheap Gemini for parsing/research; expensive Claude only for guardrail-critical generation.

---

## Tech Stack

| Layer          | Technology                                                         |
|----------------|--------------------------------------------------------------------|
| **Backend**    | Java 25 (LTS) + Spring Boot 4.1.x + Spring AI 2.0.x                |
| **Frontend**   | React 19 + TypeScript + Vite + Tailwind CSS                        |
| **Database**   | PostgreSQL + pgvector (for semantic search, later phases)          |
| **Migrations** | Flyway                                                             |
| **LLM**        | Claude Sonnet (generation), Gemini 2.5 Flash (extraction/research) |
| **MCP**        | Spring AI MCP client/server for tool orchestration                 |
| **Docker**     | Compose for local Postgres + pgvector                              |
| **CI/CD**      | GitHub Actions                                                     |

---

## Prerequisites

### Required

#### Java 25 (LTS)
```bash
# macOS
brew install openjdk@25

# Linux (Ubuntu/Debian)
apt-get install openjdk-25-jdk

# Windows
# Download from https://adoptium.net/ or https://www.azul.com/downloads/
# Add to PATH, then verify:
java -version
```

#### Node.js 22+
```bash
# From https://nodejs.org/
node -v && npm -v
# Should output v22.x.x and 10.x.x+
```

#### Docker & Docker Compose
```bash
# macOS/Windows: Install Docker Desktop (includes Compose)
# Linux:
apt-get install docker.io docker-compose

# Verify:
docker --version && docker compose --version
```

#### Gradle (included via wrapper)
The project uses `./gradlew` (macOS/Linux) or `.\gradlew.bat` (Windows). No separate install needed.

### API Keys (Free Tier)

#### Anthropic (Claude)
1. Sign up: https://console.anthropic.com/
2. Create API key: https://console.anthropic.com/account/keys
3. Free $5 credit for testing

#### Google Gemini
1. Sign up: https://ai.google.dev/
2. Create API key: https://aistudio.google.com/app/apikey
3. Free tier (rate-limited, no credit card required)

---

## Quick Start

### 1. Clone & Setup

```bash
# Clone repository
git clone https://github.com/nvk1152/ai-job-search-assistant.git
cd ai-job-search-assistant

# Copy environment template
cp .env.example .env

# Edit .env with your API keys (see section below)
# macOS/Linux:
nano .env
# Windows:
# Open .env in your editor and fill in the blanks
```

### 2. Start Postgres

```bash
docker compose up -d

# Wait 3 seconds for Postgres to be ready
sleep 3  # or just wait a moment on Windows

# Verify it's running
docker compose logs postgres
# Look for: "database system is ready to accept connections"
```

### 3. Start Backend

```bash
./gradlew :app-orchestrator:bootRun
# Starts on http://localhost:8080
# Runs Flyway migrations automatically
```

### 4. Start Frontend (in a new terminal)

```bash
cd frontend
npm install  # First time only
npm run dev
# Opens http://localhost:5173
```

**Done!** You now have:
- Backend API on `http://localhost:8080`
- React UI on `http://localhost:5173`
- Postgres on `localhost:2511`

---

## Environment Setup

Create `.env` in the project root (copy from `.env.example`):

```bash
# Database
DB_HOST=localhost
DB_PORT=2511                    # Non-standard port to avoid conflicts
DB_NAME=jsa
DB_USER=jsa
DB_PASSWORD=jsa                 # ⚠️ Change in production!

# LLM APIs
ANTHROPIC_API_KEY=sk-ant-...    # From https://console.anthropic.com/keys
GEMINI_API_KEY=AIzaSy...        # From https://aistudio.google.com/apikey
```

**Never commit `.env` with real keys!** It's git-ignored for safety.

---

## Running Locally

### Backend

**Start Spring Boot app:**
```bash
./gradlew :app-orchestrator:bootRun
```

**Health check:**
```bash
curl http://localhost:8080/actuator/health
# Response: {"status":"UP"}
```

**Build without running:**
```bash
./gradlew build -x test
```

### Frontend

**Start Vite dev server:**
```bash
cd frontend
npm run dev
```

**Build for production:**
```bash
npm run build
# Output: dist/ directory
```

**Run tests:**
```bash
npm run test
```

### Database

**Start Postgres + pgvector:**
```bash
docker compose up -d
```

**View logs:**
```bash
docker compose logs -f postgres
```

**Connect to database (for debugging):**
```bash
docker compose exec postgres psql -U jsa -d jsa
# Then: \dt (list tables), SELECT * FROM profile; etc.
```

**Stop & clean up:**
```bash
docker compose down
docker compose down -v  # Also delete data
```

---

## Testing

### Backend Tests
```bash
# All tests (requires Docker for Testcontainers)
./gradlew test

# Specific test class
./gradlew :app-orchestrator:test --tests '*ModelRouter*'

# Build only (skip tests for speed)
./gradlew build -x test
```

**Note:** Backend tests do **not** require real `ANTHROPIC_API_KEY`/`GEMINI_API_KEY` values. A
`test` Spring profile (`app-orchestrator/src/test/resources/application-test.yaml`) supplies harmless
placeholder keys so Spring context-loading tests can start without live credentials — Docker (for
Testcontainers Postgres) is the only prerequisite.

### Frontend Tests
```bash
cd frontend
npm run test
```

---

## Project Structure

```
ai-job-search-assistant/
├── common/                          # Shared DTOs
│   └── src/main/java/.../
│       ├── Profile.java
│       ├── Experience.java
│       ├── Education.java
│       ├── Skill.java
│       ├── JobDescription.java
│       └── Application.java
│
├── app-orchestrator/                # Main backend (Spring Boot)
│   ├── src/main/java/.../
│   │   ├── llm/
│   │   │   ├── ModelRouter.java         # LLM routing: Claude/Gemini by task
│   │   │   ├── LlmTask.java             # Task type enum
│   │   │   ├── LlmCallAudit.java        # Audit trail record
│   │   │   └── ModelRoutingProperties.java
│   │   ├── resume/
│   │   │   ├── ResumeExtractionService.java  # No-fabrication guardrail
│   │   │   ├── ResumeTextExtractor.java      # PDF/DOCX → text
│   │   │   └── ExtractedProfileData.java
│   │   ├── profile/
│   │   │   ├── ProfileService.java      # CRUD with user isolation
│   │   │   ├── ProfileController.java   # REST endpoints
│   │   │   ├── ProfileRepository.java
│   │   │   └── ExperienceRepository, SkillRepository, ...
│   │   └── error/
│   │       └── ApiExceptionHandler.java
│   ├── src/test/java/.../
│   │   ├── ModelRouterTest.java
│   │   ├── ResumeExtractionServiceTest.java
│   │   ├── ResumeTextExtractorTest.java
│   │   ├── ProfileServiceIntegrationTest.java
│   │   ├── ProfileControllerTest.java
│   │   └── LlmCallAuditRepositoryIntegrationTest.java
│   ├── src/test/resources/
│   │   └── application-test.yaml    # placeholder LLM keys for the `test` profile
│   └── src/main/resources/
│       ├── application.yaml
│       └── db/migration/
│           ├── V1__enable_pgvector.sql
│           ├── V2__profile_schema.sql
│           └── V3__llm_call_audit.sql
│
├── mcp-profile-server/              # MCP server (Phase 3+)
│   └── src/main/java/.../ProfileMcpServerApplication.java
│
├── frontend/                        # React + TypeScript + Vite
│   ├── src/
│   │   ├── pages/
│   │   │   └── ProfileEditor.tsx     # Main page
│   │   ├── components/
│   │   │   ├── ExperienceList.tsx
│   │   │   ├── SkillList.tsx
│   │   │   ├── EducationList.tsx
│   │   │   └── ResumeUpload.tsx
│   │   ├── api/
│   │   │   └── profileApi.ts        # HTTP client
│   │   ├── types/
│   │   │   └── profile.ts
│   │   └── main.tsx
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
│
├── docker-compose.yml               # Postgres + pgvector
├── .env.example                     # Environment template
├── .github/workflows/ci.yml         # GitHub Actions CI
├── build.gradle.kts                 # Gradle root config
├── settings.gradle.kts              # Gradle multi-module
├── CLAUDE.md                        # Architecture & operational guide
├── docs/PROJECT.md                  # Full product spec
└── README.md                        # This file
```

---

## API Reference

All endpoints on `http://localhost:8080/api/profile`.

### GET /api/profile
Fetch user's profile.

**Response:**
```json
{
  "profile": {
    "id": "uuid",
    "userId": "uuid",
    "headline": "Software Engineer",
    "summary": "5+ years building..."
  },
  "experiences": [...],
  "skills": [...],
  "education": [...]
}
```

### PUT /api/profile
Save edited profile.

**Request body:** Same as GET response.

### POST /api/profile/import
Upload résumé (PDF/DOCX) → extract via LLM → return structured data.

**Request:** `multipart/form-data` with file field.

**Response:** Same as GET (pre-filled from extraction).

---

## Phase 1 Status

### ✅ Implemented
- Profile CRUD (view, edit, save)
- Résumé import (PDF/DOCX)
- LLM extraction with no-fabrication guardrail
- Both model providers wired (Claude + Gemini)
- Model routing by task type + extraction fallback
- Database schema + Flyway migrations
- REST API
- React profile editor UI
- Unit & integration tests
- GitHub Actions CI
- Manual code-quality pass: transactional profile save, structured error handling
  (`ApiExceptionHandler` covers bad input and unexpected failures, not just upload/extraction errors),
  logged extraction fallback, extractor/controller/guardrail-prompt test coverage, and a fixed CI
  pipeline (backend tests no longer require real LLM API keys; frontend build/test issues resolved).
  Real SonarQube/SonarCloud integration is deferred to Phase 7 (Hardening/ops) rather than done here.

### ❌ Not Yet (Planned for later phases)
- User authentication (Phase 6 — Google OIDC)
- Job description intake (Phase 2)
- Fit-gap analysis (Phase 4)
- Tailored résumé/cover letter generation (Phase 4)
- Interview talking points (Phase 4)
- Kanban job tracker (Phase 5)
- Web research & company brief (Phase 4)

See [`CLAUDE.md` §8](CLAUDE.md#8-phase-plan--build-one-demoable-slice-at-a-time) for full roadmap.

---

## Guardrails & Safety

This project enforces hard constraints:

### 1. No Fabrication
Résumé extraction uses an explicit no-fabrication prompt. The LLM extracts only facts present in the résumé—never invents employers, titles, skills, or dates.

**Test:** Upload a résumé without "Java" skill → extraction won't claim you know Java.

### 2. Grounded Company Claims
Any assertion about a company comes from web search (Phase 4+) and includes the source URL.

### 3. Server-Owned Identity
User ID is injected server-side from the database, never accepted from client input. Every profile query is filtered by authenticated user.

### 4. Audit Trail
All LLM calls logged to `llm_call_audit` table: task type, model, tokens used, latency, success/failure.

### 5. Human-in-the-Loop
All AI outputs are **drafts**. Nothing auto-sends; everything requires explicit user approval.

See [`CLAUDE.md` §6](CLAUDE.md#6-guardrails--hard-invariants-never-violate-call-these-out-in-prs) for detailed guardrail spec.

---

## Troubleshooting

| Problem                           | Solution                                                                                       |
|-----------------------------------|------------------------------------------------------------------------------------------------|
| `PostgreSQL connection refused`   | Run `docker compose up -d`; wait 3s; check `docker compose logs postgres`                      |
| `ANTHROPIC_API_KEY empty` error (running the app) | Add key to `.env`; restart backend                                             |
| `Gemini API key not valid`        | Ensure key is from https://aistudio.google.com/apikey (not Vertex)                             |
| Frontend can't connect to backend | Ensure backend is on port 8080; check browser console for CORS/network errors                  |
| Tests fail to start Spring context | Backend tests use a `test` profile with placeholder keys and don't need real credentials — this indicates a genuine config/wiring problem, not a missing `.env`. Check Docker is running for Testcontainers. |
| Port 5173 in use                  | Kill other process: `lsof -i :5173` (macOS/Linux) or `netstat -ano \| findstr :5173` (Windows) |
| Port 8080 in use                  | Restart with: `./gradlew :app-orchestrator:bootRun --args='--server.port=9090'`                |
| Docker not running                | Start Docker Desktop (macOS/Windows) or `systemctl start docker` (Linux)                       |

---

## Contributing

### Branch Naming
- Feature: `feature/your-feature-name`
- Bugfix: `bugfix/issue-description`
- Task: `task/what-youre-doing`

### Commit Message Format
```
<component>: <brief description>

Longer explanation if needed.

Related issues: #123
```

Examples:
```
profile: add headline validation
resume: fix bullet point parsing on edge case
llm: add retry logic for rate limits
```

### Before Pushing
```bash
# Build & check syntax
./gradlew build -x test

# Frontend
cd frontend && npm run build

# All tests (if Docker running)
./gradlew test && npm run test
```

### Pull Request
- Title: `<component>: <description>`
- Description: Link to issue, explain why the change is needed
- At least one review before merge

---

## Documentation

- **[CLAUDE.md](CLAUDE.md)** — Architecture guide, operational rules, phase plan
- **[docs/PROJECT.md](docs/PROJECT.md)** — Full product specification
- **Inline comments** — Code has 1–4 line explanatory comments on key classes

---

## Support

- **GitHub Issues:** Report bugs or request features
- **Discussions:** Ask questions
- **Email:** [nvk1152@gmail.com](mailto:nvk1152@gmail.com)

---

## License

MIT License — see [LICENSE](LICENSE) file.

---

**Built with ❤️ for developers who want to automate job applications without sacrificing integrity.**
