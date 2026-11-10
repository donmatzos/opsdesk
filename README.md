# opsdesk

[![ci](https://github.com/donmatzos/opsdesk/actions/workflows/ci.yml/badge.svg)](https://github.com/donmatzos/opsdesk/actions/workflows/ci.yml) [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=donmatzos_opsdesk&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=donmatzos_opsdesk) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=donmatzos_opsdesk&metric=coverage)](https://sonarcloud.io/summary/new_code?id=donmatzos_opsdesk)

> Work in progress. Skeleton only; see `docs/context/status.md`.

A minimal service-status and incident desk: Spring Boot (Java 25) backend, Angular frontend, an MCP server for AI assistants, shipped as one jar and operated like a production service (CI/CD, container, Helm, Terraform, observability, security scans).

Cloud parts (Azure OpenAI / AI Search, OpenShift, Datadog, Artifactory) are mocked or disabled, because the project runs without a cloud account. They are written the way they would be used, and each one is marked.

## Use it from Claude Code
The backend serves an MCP server at `/mcp` (6 tools: services, incidents, open/change incident). It needs a bearer token, so generate one and start the stack:

```bash
cp deploy/compose/.env.example deploy/compose/.env   # set the DB password, then add: OPSDESK_MCP_TOKEN=$(openssl rand -hex 32)
docker compose -f deploy/compose/compose.yml --profile app up -d --build --wait
set -a; . deploy/compose/.env; set +a
tools/mcp-verify.sh      # MCP Inspector check, prints MCP VERIFY PASS
claude                   # the repo's .mcp.json registers the server "opsdesk"; ask "which services are degraded?"
```

Details and Claude Desktop setup: `docs/context/mcp.md`.

<!-- TODO: architecture diagram, quick start, "what this shows" table (from docs/context/skills-matrix.md) -->
