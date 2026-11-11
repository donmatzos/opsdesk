# opsdesk

[![ci](https://github.com/donmatzos/opsdesk/actions/workflows/ci.yml/badge.svg)](https://github.com/donmatzos/opsdesk/actions/workflows/ci.yml) [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=donmatzos_opsdesk&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=donmatzos_opsdesk) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=donmatzos_opsdesk&metric=coverage)](https://sonarcloud.io/summary/new_code?id=donmatzos_opsdesk)

> Work in progress. Skeleton only; see `docs/context/status.md`.

A minimal service-status and incident desk: Spring Boot (Java 25) backend, Angular frontend, an MCP server for AI assistants, shipped as one jar and operated like a production service (CI/CD, container, Helm, Terraform, observability, security scans).

Cloud parts (Azure OpenAI / AI Search, OpenShift, Datadog, Artifactory) are mocked or disabled, because the project runs without a cloud account. They are written the way they would be used, and each one is marked.

## Use it from Claude Code
The backend serves an MCP server at `/mcp` (7 tools: services, incidents, open/change incident, search runbooks). It needs a bearer token, so generate one and start the stack:

```bash
cp deploy/compose/.env.example deploy/compose/.env
# edit deploy/compose/.env: set the DB password, and paste the output of `openssl rand -hex 32` after OPSDESK_MCP_TOKEN=
docker compose -f deploy/compose/compose.yml --profile app up -d --build --wait
set -a; . deploy/compose/.env; set +a
tools/mcp-verify.sh      # MCP Inspector check, prints MCP VERIFY PASS
claude                   # the repo's .mcp.json registers the server "opsdesk"; ask "which services are degraded?"
```

Details (tools, auth, Claude Desktop notes): `docs/context/mcp.md`.

## Ask the runbooks
`docs/runbooks/` holds the operations runbooks; they ship inside the jar and are indexed in memory (BM25). `POST /api/assist` and the MCP tool `search_runbooks` return the best-matching sections with sources:

```bash
curl -s localhost:8080/api/assist -H 'Content-Type: application/json' -d '{"question":"database connection refused"}'
```

**Not for real use as it is:** `/api/assist` has no authentication and no rate limit. That is harmless with the mock, but before running it against real Azure services it must be closed and/or throttled.

The answer is composed by a **mock** model (`"mock": true`) unless profile `azure` is set. That profile switches to Azure OpenAI and Azure AI Search adapters, which have only been tested against a local fake server, plus Terraform in `infra/terraform/azure` that is validated in CI but never applied. See `docs/context/cloud-mocks.md`.

<!-- TODO: architecture diagram, quick start, "what this shows" table (from docs/context/skills-matrix.md) -->
