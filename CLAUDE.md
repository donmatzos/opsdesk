# opsdesk

Goal: a small, public showcase project for recruiters. A service-status and incident desk (Spring Boot + Angular, one jar) with an MCP server, built and operated like a production service: CI/CD, container, Helm/OpenShift, IaC, observability, DevSecOps. Owner: DevOps engineer; the ops side matters as much as the code.

## Read order (about 1 500 tokens, then only what the task needs)
1. This file. 2. `docs/context/INDEX.md` (one line per context file). 3. `docs/context/status.md`. 4. The files for your task (table below). Never load the whole `docs/` tree.

## Hard rules
- Never commit to `main`. Branches: `feature/|fix/|update/<kebab-name>`. Stage explicit paths only. No PR/merge unless asked.
- No real cloud access exists. Azure, OpenShift, Datadog, Artifactory parts are **mocked or disabled by default** and must say so in a comment. Rules: `docs/context/cloud-mocks.md`.
- No secrets in the repo, not even fake-looking ones. Use env vars / CI secrets placeholders.
- `mvn verify` must be green before each commit. Backend tests: TestNG + Mockito (no JUnit).
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

## Commands (planned, not yet working)
`mvn verify` (all modules, frontend via frontend-maven-plugin) · `java -jar backend/target/opsdesk.jar` · `docker compose -f deploy/compose/compose.yml up`.
