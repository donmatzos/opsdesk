# MCP server (phase 5, done)
Purpose: let an AI assistant (Claude Code/Desktop, Copilot) look up services and work on incidents, mirroring the owner's work MCP server.

## Transport and auth
- Spring AI 2.0.2 MCP server inside the backend jar (no separate module), Streamable HTTP at `POST /mcp` on :8080. `protocol: STATELESS`: no session in the pod, any replica answers (no sticky routing). Code: `backend/src/main/java/at/donmatzos/opsdesk/mcp/`.
- Auth: `Authorization: Bearer <token>`. Token from env `OPSDESK_MCP_TOKEN` (`opsdesk.mcp.token`). Unset/empty -> `/mcp` answers 503 (MCP off, rest of the app unaffected); missing or wrong token -> 401. Constant-time compare in `McpTokenFilter`.
- Generate a token: `openssl rand -hex 32`. Local: put it in `deploy/compose/.env` (git-ignored). Cluster: Secret with key `token`, Helm `mcp.tokenSecret` = its name (env via `secretKeyRef`). Never commit a token.

## Tools (tool descriptions are written for the model)
| Tool | Parameters | Notes |
|---|---|---|
| `list_services` | `stage?` (DEV, PROD) | id, name, ownerTeam, stage, status; read-only |
| `get_service_status` | `service_name` | exact, case-sensitive; unknown name -> tool error |
| `list_active_incidents` | `severity?` (SEV1..SEV3) | OPEN/MITIGATED, newest first, max 50, `truncated` flag |
| `get_incident` | `incident_id` | unknown id -> tool error |
| `open_incident` | `service_name`, `severity`, `summary` (1-200 chars) | writes; service by name |
| `change_incident_state` | `incident_id`, `state` | OPEN->MITIGATED->RESOLVED, OPEN->RESOLVED; RESOLVED is final |
Tools call `IncidentService`, so validation, transitions, metrics and ECS logs match the REST API. Domain errors (not found, conflict, invalid value) come back as tool results with `isError: true` and a readable message; anything unexpected is logged and reported generically (no stack traces). `search_runbooks` arrives in phase 6.

## Connect
Start the app with a token: `docker compose -f deploy/compose/compose.yml --profile app up -d --build --wait` (token in `.env`).
- Claude Code: root `.mcp.json` (project scope) holds server `opsdesk` and `${OPSDESK_MCP_TOKEN}`; export the token, then run `claude` in the repo and approve the server (`/mcp` shows it).
- Claude Desktop: no env expansion in remote servers; use `npx mcp-remote http://localhost:8080/mcp --header "Authorization: Bearer <token>"` in `claude_desktop_config.json` (local file only).
- Inspector / CI-style check: `set -a; . deploy/compose/.env; set +a; tools/mcp-verify.sh` (401 without token, 6 tools, 4 demo services, prints `MCP VERIFY PASS`). Interactive: `npx @modelcontextprotocol/inspector@2.14.0`, transport Streamable HTTP, header `Authorization`.
