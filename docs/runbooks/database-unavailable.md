# Database unavailable

Use this when opsdesk cannot reach PostgreSQL: `/readyz` fails, the pods leave the service and requests return 5xx.

## Symptoms
- Postgres connection refused or connection timeout in the logs (`Unable to acquire JDBC Connection`, `HikariPool` timeouts).
- `/readyz` returns 503 while `/livez` stays 200; pods show `0/1 Ready`.
- Flyway fails at startup with a connection error.

## First checks
- Is the database pod running? `oc get pods` and `oc logs <postgres-pod>` (dev stage: the in-namespace PostgreSQL from the chart).
- Is the URL right? Check `SPRING_DATASOURCE_URL` in the deployment: `oc set env deployment/opsdesk --list` (never print the password).
- Did the password Secret change? The Secret named in `database.existingSecret` (or the generated dev Secret) must match the database.
- Connectivity: NetworkPolicy must allow the opsdesk pods to reach port 5432; test from a pod with `nc -zv <host> 5432`.
- Disk full or max connections reached on the database side.

## Mitigation
- Restart a crashed database pod: `oc rollout restart deployment/<postgres>`; opsdesk reconnects on its own.
- Wrong Secret: fix the Secret, then `oc rollout restart deployment/opsdesk`.
- Too many connections: reduce replicas temporarily, then find the leak.

## Escalation
- Database down for more than 10 minutes: open SEV1 (`sev1-incident`) and page the database owner.
- Suspected data loss: stop writes and escalate immediately; do not restore without the owner.

## Related
- `pod-not-ready`, `http-errors`, `sev1-incident`
