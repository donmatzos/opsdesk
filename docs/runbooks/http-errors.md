# HTTP errors (5xx)

Use this when the share of 5xx responses in `http.server.requests` rises (Datadog monitor "HTTP 5xx rate", `infra/terraform/datadog/monitors.tf`).

## Symptoms
- The metric `http.server.requests` with `status` 5xx grows, or users see error pages.
- Log lines at ERROR level appear with an exception class and a `trace.id`.

## First checks
- Which endpoint? Group `http.server.requests` by `uri` and `status` (Prometheus on the actuator port 9090, `/actuator/prometheus`).
- Was there a deploy? `helm history opsdesk`; an error spike right after a deploy points to the new version.
- Search Kibana for `log.level: ERROR` and open one `trace.id` to see the whole request.
- Check `/readyz` on port 8080: if it fails, the database or a dependency is unreachable (see `database-unavailable`).

## Mitigation
- Spike after a deploy: `helm rollback opsdesk <revision>`.
- Only one endpoint failing: check the cause in the logs, then fix forward or disable the caller.
- Overload (high CPU, slow responses): scale up with `oc scale deployment/opsdesk --replicas=<n>` or let the HPA do it.

## Escalation
- 5xx above 5% for 10 minutes: open a SEV2 incident; above 25% or the service fully down: open SEV1 (`sev1-incident`).

## Related
- `sev1-incident`, `database-unavailable`, `pod-not-ready`
