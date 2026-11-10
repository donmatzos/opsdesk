# Architecture (decisions: `docs/adr/0001-initial-decisions.md`)

## Modules (Maven)
| Module | Role | Depends on |
|---|---|---|
| `frontend` | Angular app, built by frontend-maven-plugin into `META-INF/resources` (or `static/`) of a jar | - |
| `ai` | RAG: `RunbookIndex`, `Retriever`, `LlmClient`; mock + Azure adapters (profile `azure`, disabled) | - |
| `backend` | Spring Boot app: REST API, domain, MCP server (package `mcp`, `/mcp`), actuator; packages everything into `opsdesk.jar` | `frontend`, `ai` |

## Domain (minimal)
- `MonitoredService` in package `catalog` (name, owner team, stage, status UP/DEGRADED/DOWN). Not `Service`: clashes with Spring's `@Service`.
- `Incident` (id, service, severity, state OPEN/MITIGATED/RESOLVED, timeline).
- `Runbook` (markdown files from `docs/runbooks/`, indexed for RAG).

Persistence: PostgreSQL, Spring Data JDBC, Flyway (`db/migration`; demo data in `db/demo`, profile `demo`).

## API (phase 1)
`GET /api/services`, `GET /api/incidents?state=`, `POST /api/incidents`, `PATCH /api/incidents/{id}`, `POST /api/assist` (RAG question -> answer + sources). Errors as RFC 9457 `ProblemDetail`.

## Spring profiles
`default` (all local, mocks), `azure` (Azure adapters, disabled: no credentials), `datadog` (Datadog registry, disabled), `k8s` (probes/logging tuned for cluster).

## Data flow
Browser -> Angular -> `/api/*` -> domain services. AI client -> `/mcp` (Spring AI MCP server, streamable HTTP, same jar) -> MCP tools (package `mcp`, bearer token, stateless) -> same domain services (`IncidentService`, repositories). `/api/assist` -> `Retriever` -> `LlmClient` (mock or Azure OpenAI).
