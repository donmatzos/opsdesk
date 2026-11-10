#!/usr/bin/env bash
# Smoke test against a running opsdesk: health UP, API answers JSON, UI is served.
# Usage: tools/smoke-test.sh <base-url> [timeout-seconds]   e.g. tools/smoke-test.sh http://localhost:8080
set -euo pipefail

base_url=${1:?usage: smoke-test.sh <base-url> [timeout-seconds]}
timeout=${2:-60}

fail() { echo "SMOKE FAIL: $*" >&2; exit 1; }

echo "Waiting up to ${timeout}s for ${base_url}/actuator/health ..."
deadline=$((SECONDS + timeout))
until [[ $(curl -fsS --max-time 5 "${base_url}/actuator/health" 2>/dev/null) == *'"status":"UP"'* ]]; do
  (( SECONDS < deadline )) || fail "health not UP after ${timeout} s"
  sleep 2
done
echo "ok  health UP"

readiness=$(curl -fsS --max-time 5 "${base_url}/actuator/health/readiness") || fail "GET /actuator/health/readiness failed"
[[ $readiness == *'"status":"UP"'* ]] || fail "readiness probe not UP: ${readiness:0:200}"
echo "ok  readiness probe UP"

services=$(curl -fsS --max-time 10 -H 'Accept: application/json' "${base_url}/api/services") || fail "GET /api/services failed"
[[ $services == \[* ]] || fail "GET /api/services did not return a JSON array: ${services:0:200}"
echo "ok  /api/services returns JSON"

index=$(curl -fsS --max-time 10 "${base_url}/") || fail "GET / failed"
[[ $index == *'<app-root'* ]] || fail "GET / does not serve the Angular app"
echo "ok  / serves the Angular app"

echo "SMOKE PASS: ${base_url}"
