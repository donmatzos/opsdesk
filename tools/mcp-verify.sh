#!/usr/bin/env bash
# End-to-end check of the MCP server: the running app must expose /mcp with the seven tools, answer a tool call
# with real data and reject requests without the bearer token. Uses the MCP Inspector CLI (npx, needs Node).
# Usage: OPSDESK_MCP_TOKEN=<token> tools/mcp-verify.sh [base-url]   (default http://localhost:8080)
# The app must run with the same token, e.g. compose profile "app" with OPSDESK_MCP_TOKEN in deploy/compose/.env.
set -euo pipefail

base=${1:-http://localhost:8080}
url="${base}/mcp"
inspector=(npx -y @modelcontextprotocol/inspector@2.14.0)

fail() { echo "MCP VERIFY FAIL: $*" >&2; exit 1; }
[[ -n ${OPSDESK_MCP_TOKEN:-} ]] || fail "OPSDESK_MCP_TOKEN is not set (export the token the app was started with)"

echo "== without token: expect 401"
status=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 10 -X POST "${url}" \
  -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}') || fail "no answer from ${url}"
[[ ${status} == 401 ]] || fail "POST /mcp without token returned ${status}, expected 401"
echo "ok  401 without Authorization header"

# The header goes through a variable and Inspector stderr is discarded, so the token never appears in output.
auth="Authorization: Bearer ${OPSDESK_MCP_TOKEN}"
call() { "${inspector[@]}" --cli "${url}" --transport http --header "${auth}" "$@" 2>/dev/null; }

echo "== tools/list via MCP Inspector"
tools_json=$(call --method tools/list) || fail "tools/list failed (wrong token or app not running with it?)"
python3 - "${tools_json}" <<'PY' || fail "unexpected tool list"
import json, sys
names = sorted(t["name"] for t in json.loads(sys.argv[1])["tools"])
expected = sorted(["list_services", "get_service_status", "list_active_incidents", "get_incident",
                   "open_incident", "change_incident_state", "search_runbooks"])
assert names == expected, f"tools {names}, expected {expected}"
print(f"ok  {len(names)} tools: {', '.join(names)}")
PY

echo "== tools/call list_services"
services_json=$(call --method tools/call --tool-name list_services) || fail "tools/call list_services failed"
python3 - "${services_json}" <<'PY' || fail "unexpected list_services result"
import json, sys
result = json.loads(sys.argv[1])
assert not result.get("isError"), result
services = json.loads(result["content"][0]["text"])
assert len(services) == 4, f"{len(services)} services, expected 4 (demo data)"
print(f"ok  {len(services)} services: {', '.join(s['name'] for s in services)}")
PY
echo "== tools/call search_runbooks"
runbooks_json=$(call --method tools/call --tool-name search_runbooks --tool-arg "query=pod not ready") \
  || fail "tools/call search_runbooks failed"
python3 - "${runbooks_json}" <<'PY' || fail "unexpected search_runbooks result"
import json, sys
result = json.loads(sys.argv[1])
assert not result.get("isError"), result
hits = json.loads(result["content"][0]["text"])["hits"]
assert any(h["runbook"] == "pod-not-ready" for h in hits), f"no pod-not-ready hit: {hits}"
print(f"ok  {len(hits)} runbook hits, first: {hits[0]['runbook']}")
PY
echo "MCP VERIFY PASS"
