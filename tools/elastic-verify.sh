#!/usr/bin/env bash
# End-to-end check of the local Elastic stack (compose profile "observability"): start app + Elasticsearch +
# Kibana + Filebeat, create an incident, wait until its ECS log lines are searchable with a trace.id, and
# create the Kibana data view. Usage: tools/elastic-verify.sh [--down]   (--down stops the stack afterwards)
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
compose=(docker compose -f "${root}/deploy/compose/compose.yml" --profile app --profile observability)
es=http://127.0.0.1:9200
kibana=http://127.0.0.1:5601
app=http://127.0.0.1:8080

fail() { echo "ELASTIC VERIFY FAIL: $*" >&2; exit 1; }
if [[ ${1:-} == --down ]]; then trap '"${compose[@]}" down' EXIT; fi

echo "== start app + elastic stack"
"${compose[@]}" up -d --build --wait

# Review Focus 5: nothing beyond localhost.
for svc in elasticsearch kibana; do
  bound=$("${compose[@]}" port "${svc}" "$([[ ${svc} == elasticsearch ]] && echo 9200 || echo 5601)")
  [[ ${bound} == 127.0.0.1:* ]] || fail "${svc} is bound to ${bound}, expected 127.0.0.1"
done
echo "ok  elasticsearch and kibana listen on 127.0.0.1 only"

echo "== create an incident and change its state (two requests, two log lines)"
id=$(curl -fsS -X POST "${app}/api/incidents" -H 'Content-Type: application/json' \
  -d '{"serviceId":1,"severity":"SEV2","summary":"elastic verify"}' | python3 -c 'import json,sys;print(json.load(sys.stdin)["id"])')
curl -fsS -o /dev/null -X PATCH "${app}/api/incidents/${id}" -H 'Content-Type: application/json' -d '{"state":"MITIGATED"}'

echo "== wait for the log lines of incident ${id} in Elasticsearch"
deadline=$((SECONDS + 120))
hits=""
until [[ -n ${hits} ]]; do
  (( SECONDS < deadline )) || fail "no log lines for incident ${id} after 120 s"
  sleep 3
  hits=$(curl -fsS "${es}/opsdesk-logs/_search?q=incident.id:${id}&size=10" 2>/dev/null \
    | python3 -c '
import json, sys
docs = [h["_source"] for h in json.load(sys.stdin)["hits"]["hits"]]
if len(docs) >= 2:
    print(json.dumps(docs))' || true)
done
python3 - "${hits}" <<'PY' || fail "log lines are incomplete"
import json, sys
docs = json.loads(sys.argv[1])
for d in docs:
    trace = d.get("trace", {}).get("id")
    assert d.get("service", {}).get("name") == "opsdesk", d
    assert trace and len(trace) == 32, f"missing trace.id: {d.get('message')}"
    print(f"ok  {d['message']:<24} trace.id={trace}")
assert len({d['trace']['id'] for d in docs}) == len(docs), "two requests must have two traces"
PY

curl -fsS "${es}/_data_stream/opsdesk-logs" | grep -q '"name":"opsdesk-logs"' || fail "opsdesk-logs is not a data stream"
echo "ok  logs land in the data stream opsdesk-logs"

echo "== Kibana data view opsdesk-logs"
if ! curl -fsS "${kibana}/api/data_views" | grep -q '"title":"opsdesk-logs"'; then
  curl -fsS -o /dev/null -X POST "${kibana}/api/data_views/data_view" -H 'kbn-xsrf: true' \
    -H 'Content-Type: application/json' \
    -d '{"data_view":{"title":"opsdesk-logs","name":"opsdesk logs","timeFieldName":"@timestamp"}}'
fi
echo "ok  data view ready: ${kibana}/app/discover (search e.g. trace.id : \"<id>\" or incident.id : ${id})"
echo "ELASTIC VERIFY PASS"
