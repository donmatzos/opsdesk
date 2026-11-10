# Phase 5: MCP Server Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** opsdesk exposes its service catalog and incident workflow as MCP tools at `/mcp` (Streamable HTTP, inside the same jar), protected by a bearer token, usable from Claude Code / Claude Desktop / MCP Inspector, with the same domain rules, metrics and logs as the REST API.

**Architecture:** Spring AI's MCP server starter (WebMVC) in the `backend` module, package `at.donmatzos.opsdesk.mcp`. Tool classes are thin adapters over the existing `IncidentService` and `MonitoredServiceRepository` (no new business logic). Transport: Streamable HTTP in **stateless** mode (no session affinity, works behind a Route with several replicas). A servlet filter guards `/mcp` with a bearer token from the environment (`OPSDESK_MCP_TOKEN`); without the variable the MCP endpoint is off (503) and the rest of the app is unaffected. The empty `mcp/` Maven module skeleton is removed (a separate module would need the backend domain and be packaged by the backend: a cycle).

**Tech Stack:** Spring AI 2.0.2 (`spring-ai-bom`, `spring-ai-starter-mcp-server-webmvc`, MCP annotations), MCP Java SDK (client side in tests), MCP Inspector 2.14.0 (CLI mode, local e2e only).

**Spec:** `docs/plans/2026-11-10-roadmap.md` (row 5), `docs/adr/0001-initial-decisions.md` (MCP in the jar, Spring AI, `/mcp`), `docs/context/mcp.md`, `docs/context/architecture.md`.

## Global Constraints
- No secrets in the repo, ever: the MCP token only from env (`OPSDESK_MCP_TOKEN`, local `deploy/compose/.env`, cluster Secret); `.mcp.json` references it as `${OPSDESK_MCP_TOKEN}`; tests generate a random token at runtime.
- Tool names `snake_case`; every tool and every parameter has a description written for the model: what it returns, **when to call it**, valid values. Tool annotations set (`readOnlyHint`, `destructiveHint`, `idempotentHint`, `openWorldHint: false`).
- Tool results are compact JSON (records), lists capped at 50 entries with a `truncated` flag; no stack traces or SQL in results.
- Domain errors (unknown service/incident, illegal transition, invalid enum) are returned as MCP tool errors (`isError: true`, clear message), never as transport/500 errors.
- Tools call the existing services, so validation, transitions, after-commit metrics and ECS logs apply unchanged.
- Branch `feature/mcp-server`; one commit per task.

## Review Focus
1. `/mcp` without or with a wrong token returns 401 and runs nothing; the comparison is constant-time; with `OPSDESK_MCP_TOKEN` unset `/mcp` is 503 while the REST API and UI still work (Task 3).
2. Invalid tool input (unknown severity, unknown service/incident id, illegal transition) yields a tool error with a helpful message, not a 500 (Task 2).
3. A write through MCP behaves exactly like the REST API (metrics counter increments, log line with `incident.id`) (Task 2).
4. Stateless mode: two independent client connections work without a session handshake affinity (Task 1).
5. Large lists are capped (Task 2).

## Prerequisite check (start of Task 1)
Spring AI 2.0.2 must start with Spring Boot 4.1.2. If its auto-configuration fails on Boot 4.1 (class/property mismatches), use the newest Spring AI 2.1.x GA if released, else pin `spring-ai` 2.1.0-RC2 and ledger the ruling; do not downgrade Spring Boot.

---

### Task 1: MCP server in the jar (stateless Streamable HTTP) with a first read-only tool

**Files:**
- Modify: `pom.xml` (`dependencyManagement`: import `org.springframework.ai:spring-ai-bom:2.0.2`), `backend/pom.xml` (`spring-ai-starter-mcp-server-webmvc`; test: MCP Java SDK client `io.modelcontextprotocol.sdk:mcp` from the BOM)
- Modify: `backend/src/main/resources/application.yml` (`spring.ai.mcp.server.name: opsdesk`, `version: ${project version or 0.1.0}`, `protocol: STATELESS`, endpoint `/mcp`, `instructions:` short server description for the model)
- Create: `backend/src/main/java/at/donmatzos/opsdesk/mcp/CatalogTools.java` (`list_services(stage?)`)
- Create: `backend/src/test/java/at/donmatzos/opsdesk/mcp/McpTestClient.java` (helper: builds a synchronous MCP client with the Streamable HTTP transport against `http://localhost:<port>/mcp`), `McpServerTest.java` (extends `IntegrationTest`)
- Remove: `mcp/` (empty Maven module skeleton, never built)

**Interfaces:**
- Produces: `@Component class CatalogTools` with `@McpTool(name = "list_services", ...) List<ServiceSummary> listServices(@McpToolParam(required = false, description = ...) Stage stage)`; `record ServiceSummary(long id, String name, String ownerTeam, Stage stage, ServiceStatus status)`.
- Produces: `McpTestClient.connect(int port, String token)` -> `McpSyncClient` (initialized); the token is sent as `Authorization: Bearer <token>` (used from Task 3 on; before Task 3 the header is ignored).

- [ ] **Step 1: Write the failing test** `McpServerTest.shouldListToolsAndCallListServices`: client `listTools()` contains `list_services` with a non-empty description and `readOnlyHint == true`; `callTool("list_services", {})` returns 4 services (demo data), `isError == false`; with `{"stage":"DEV"}` only `report-batch`.
- [ ] **Step 2:** `./mvnw -B -ntp -pl backend -am verify -Dtest=McpServerTest -Dsurefire.failIfNoSpecifiedTests=false`. Expected: FAIL (no MCP endpoint).
- [ ] **Step 3:** Implement; resolve the prerequisite check. **Step 4:** Expected: PASS; full `./mvnw verify` green.
- [ ] **Step 5 (Review Focus 4):** `shouldServeIndependentClientsWithoutSessions`: two clients, each `callTool` works; a raw `POST /mcp` `tools/list` JSON-RPC request without prior `initialize` succeeds (stateless). **Commit** `feat(mcp): stateless streamable-http mcp server with list_services`.

### Task 2: Incident tools (read and write)

**Files:**
- Create: `backend/src/main/java/at/donmatzos/opsdesk/mcp/IncidentTools.java`, `mcp/McpResults.java` (shared: cap lists at 50 + `truncated`; map domain exceptions to tool errors)
- Test: `backend/src/test/java/at/donmatzos/opsdesk/mcp/IncidentToolsTest.java` (plain TestNG + Mockito: mapping, errors, cap), extend `McpServerTest`

**Interfaces:**
- Produces tools (descriptions say when to use them):
  - `get_service_status(service_name)` -> `ServiceSummary` (read-only); unknown name -> tool error `Unknown service '<name>'. Call list_services to see valid names.`
  - `list_active_incidents(severity?)` -> `IncidentList(List<IncidentSummary> incidents, boolean truncated)` (OPEN + MITIGATED, newest first, read-only)
  - `get_incident(incident_id)` -> `IncidentSummary` (read-only)
  - `open_incident(service_name, severity, summary)` -> `IncidentSummary` (write; `destructiveHint=false`, `idempotentHint=false`)
  - `change_incident_state(incident_id, state)` -> `IncidentSummary` (write; `destructiveHint=false`, `idempotentHint=false`); description lists the allowed transitions OPEN->MITIGATED->RESOLVED, OPEN->RESOLVED.
- `record IncidentSummary(long id, String service, Severity severity, IncidentState state, String summary, Instant openedAt, Instant resolvedAt)`.
- Consumes: `IncidentService.open/list/get/changeState`, `MonitoredServiceRepository` (name lookup: add `Optional<MonitoredService> findByName(String name)`).

- [ ] **Step 1: Write the failing tests.** Unit: `shouldCapIncidentListsAt50` (60 mocked -> 50 + `truncated`), `shouldExplainUnknownService`, `shouldExplainIllegalTransition` (message from `IllegalStateTransitionException`), `shouldRejectUnknownSeverity` (`"SEV9"` -> tool error listing `SEV1, SEV2, SEV3`). Integration (`McpServerTest`): `shouldOpenAndResolveAnIncidentThroughMcp` (open via tool -> `get_incident` shows OPEN -> `change_incident_state` RESOLVED -> `resolvedAt` set), **Review Focus 3** `shouldCountMcpWritesInMetrics` (`opsdesk.incidents.opened{severity=SEV1}` +1 after `open_incident`), **Review Focus 2** `shouldReturnToolErrorsNotServerErrors` (unknown incident id, illegal transition: `isError == true`, message text, server still answers next call).
- [ ] **Step 2:** run, Expected: FAIL. **Step 3:** implement. **Step 4:** Expected: PASS; full build green.
- [ ] **Step 5: Commit** `feat(mcp): incident tools with compact results and tool errors`.

### Task 3: Bearer-token protection for /mcp

**Files:**
- Create: `backend/src/main/java/at/donmatzos/opsdesk/mcp/McpTokenFilter.java` (`OncePerRequestFilter` registered for `/mcp` and `/mcp/*` only: token unset -> 503 ProblemDetail `MCP is disabled: set OPSDESK_MCP_TOKEN`; missing/wrong `Authorization: Bearer` -> 401 ProblemDetail with `WWW-Authenticate: Bearer`; compare with `MessageDigest.isEqual` on UTF-8 bytes)
- Modify: `application.yml` (`opsdesk.mcp.token: ${OPSDESK_MCP_TOKEN:}`: empty default = disabled; this is not a credential value)
- Modify: `src/test/.../IntegrationTest.java` (random token per test context via `@DynamicPropertySource`-equivalent for TestNG or a `properties` entry built from a static `UUID`; expose `protected String mcpToken()`)
- Test: `McpTokenFilterTest.java` (plain, `MockHttpServletRequest`/`MockFilterChain`), `McpSecurityTest.java` (extends `IntegrationTest`: no header -> 401, wrong token -> 401, right token -> 200; `/api/services` still 200 without token), `McpDisabledTest.java` (Spring context with empty token: `/mcp` 503, `/api/services` 200)
- Modify: Helm chart (`mcp.tokenSecret` value, optional; when set: env `OPSDESK_MCP_TOKEN` from `secretKeyRef` key `token`; schema; unit test `tests/mcp_test.yaml`), `deploy/compose/.env.example` (`OPSDESK_MCP_TOKEN=` with comment), compose `app` gets it via the existing `env_file`

**Interfaces:**
- Produces: `McpTestClient.connect(port, token)` now authenticates; tests use `mcpToken()`.

- [ ] **Step 1: Write the failing tests** (unit + integration + disabled + chart). **Step 2:** run, Expected: FAIL (no filter). **Step 3:** implement. **Step 4:** Expected: PASS; mutation check: replace `MessageDigest.isEqual` with `true` -> `McpSecurityTest` fails (restore). **Step 5: Commit** `feat(mcp): bearer token protection for the mcp endpoint`.

### Task 4: Client setup, end-to-end check and docs

**Files:**
- Create: `.mcp.json` (Claude Code project scope: server `opsdesk`, `"type": "http"`, `"url": "http://localhost:8080/mcp"`, `"headers": {"Authorization": "Bearer ${OPSDESK_MCP_TOKEN}"}`)
- Create: `tools/mcp-verify.sh` (needs the app running with a token, e.g. compose profile `app`; uses `npx -y @modelcontextprotocol/inspector@2.14.0 --cli <url> --transport http --header "Authorization: Bearer $OPSDESK_MCP_TOKEN"` for `tools/list` and `tools/call list_services`; asserts 6 tools and 4 services; asserts 401 without the header via `curl`)
- Modify: `docs/context/mcp.md` (rewrite: transport, auth, tools table, how to connect Claude Code/Desktop/Inspector), `docs/context/architecture.md` (MCP in `backend`, no `mcp` module), `docs/context/project-structure.md` (remove `mcp/` row, add `backend/.../mcp`), `docs/context/skills-matrix.md` (MCP row: done), `docs/plans/2026-11-10-roadmap.md` (phase 5 row: tools list), `docs/context/status.md`, `CLAUDE.md` (MCP command), `README.md` (short "Use it from Claude Code" section)

- [ ] **Step 1:** `docker compose ... --profile app up -d --build --wait` with `OPSDESK_MCP_TOKEN` in `.env`; `tools/mcp-verify.sh`. Expected first run before `.mcp.json`/script exist: script missing (red); after writing it: `MCP VERIFY PASS`.
- [ ] **Step 2:** gitleaks on the working tree finds only `deploy/compose/.env`; `python3 tools/context_check.py` passes.
- [ ] **Step 3: Commit** `docs(mcp): claude code setup, mcp-verify and context updates`; push; CI green.
