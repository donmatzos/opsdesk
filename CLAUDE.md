# opsdesk

Goal: a small, public showcase project for recruiters. A service-status and incident desk (Spring Boot + Angular, one jar) with an MCP server, built and operated like a production service: CI/CD, container, Helm/OpenShift, IaC, observability, DevSecOps. Owner: DevOps engineer; the ops side matters as much as the code.

## Read order (about 1 500 tokens, then only what the task needs)
1. This file. 2. `docs/context/INDEX.md` (one line per context file). 3. `docs/context/status.md`. 4. The files for your task (table below). Never load the whole `docs/` tree.

## Hard rules
- Never commit to `main`. Branches: `feature/|fix/|update/<kebab-name>`. Stage explicit paths only. No PR/merge unless asked.
- No real cloud access exists. Azure, OpenShift, Datadog, Artifactory parts are **mocked or disabled by default** and must say so in a comment. Rules: `docs/context/cloud-mocks.md`.
- **No secrets in the repo, ever** — not even local/dev passwords or fake-looking ones (no `${X:password}` fallbacks, no literal values in compose, YAML, docs or plans). Passwords come from env vars: local `deploy/compose/.env` (git-ignored, template `.env.example`), CI from GitHub Actions secrets, clusters from Secrets. Check `git diff --cached` for credentials before every commit.
- `./mvnw verify` must be green before each commit. Backend tests: TestNG + Mockito (no JUnit).
- Every context file stays <= 100 lines and 6 KB (`python3 tools/context_check.py`). Update `status.md` at the end of a work session.

## Task -> files (all in `docs/context/`)
| Task | Read |
|---|---|
| Any code change | `conventions.md`, `workflow.md`, `testing.md` |
| Where does X live / where does new X go | `project-structure.md` |
| Backend / domain / API | `architecture.md` |
| MCP server | `mcp.md`, `architecture.md` |
| AI / RAG / Azure mocks | `cloud-mocks.md`, `architecture.md` |
| Frontend | `frontend.md` |
| CI/CD, Docker, Helm, Terraform, monitoring | `ops.md`, `cloud-mocks.md` |
| Why does this project exist / what must it show | `skills-matrix.md` |
| Plan or next phase | `status.md`, `open-questions.md`, `docs/plans/` (current phase only) |

## Commands
`./mvnw verify` (all modules; needs Docker for Testcontainers) · `cp deploy/compose/.env.example deploy/compose/.env` + set a password, then `docker compose -f deploy/compose/compose.yml up -d` (Postgres) · `SPRING_PROFILES_ACTIVE=demo SPRING_DATASOURCE_PASSWORD=<same> java -jar backend/target/opsdesk.jar` (UI + API on :8080). Frontend dev: `cd frontend && npx ng serve` (proxy to :8080). Details: `docs/context/testing.md`.
