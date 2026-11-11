# SEV1 incident

Use this when a service is down for users or an incident with severity SEV1 is open in opsdesk (the Datadog monitor on `opsdesk.incidents.*` open SEV1 count fires, see `infra/terraform/datadog/monitors.tf`).

## Symptoms
- A monitored service shows status `DOWN` in the catalog and at least one SEV1 incident is `OPEN`.
- Users or other teams report an outage; error rate on `http.server.requests` is rising.

## First checks
- Open the incident in opsdesk (`GET /api/incidents?state=OPEN`) and read title, service and start time.
- Declare a single incident commander in the channel; everyone else follows that person.
- Check what changed in the last hour: last deploy (`helm history opsdesk`), config or Secret changes.
- Look at the logs in Kibana, search by `service.name: opsdesk` and a failing `trace.id`.

## Mitigation
- Roll back first, investigate second: `helm rollback opsdesk <revision>` and wait for `oc rollout status deployment/opsdesk`.
- If the database is the cause, follow the runbook `database-unavailable`; for rising 5xx follow `http-errors`.
- Move the incident to `MITIGATED` in opsdesk once users are served again, `RESOLVED` after the cause is fixed.

## Escalation
- No progress after 15 minutes: page the secondary on-call and the service owner.
- Customer impact beyond one hour: inform the management contact of the service.

## Related
- `http-errors`, `database-unavailable`, `pod-not-ready`
- Write the postmortem within five working days, using the postmortem template.
