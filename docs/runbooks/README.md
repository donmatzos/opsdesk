# Runbooks

Short procedures for known alerts and failures. They are also the input of the "Ask the runbooks" assistant (`POST /api/assist` and the MCP tool `search_runbooks`): the app packages every `*.md` here except this file and `*-template.md`. Format of a runbook: title, intro, then `## Symptoms`, `## First checks`, `## Mitigation`, `## Escalation`, `## Related`.

| Runbook | Raised by | Use when |
|---|---|---|
| [sev1-incident](sev1-incident.md) | Datadog monitor "SEV1 incident opened" (`infra/terraform/datadog`) | a service is down for users |
| [http-errors](http-errors.md) | Datadog monitor "HTTP 5xx rate" | the share of 5xx responses rises |
| [database-unavailable](database-unavailable.md) | `/readyz` failing, 5xx with connection errors | opsdesk cannot reach PostgreSQL |
| [pod-not-ready](pod-not-ready.md) | rollout stuck, `0/1 Ready` | pods are not ready or crash-loop |
| [deploy-rollback](deploy-rollback.md) | failed smoke test after a deploy | a rollout must be undone |
| [mcp-token-rotation](mcp-token-rotation.md) | schedule, suspected leak | the MCP bearer token is rotated |
| [secret-exposure](secret-exposure.md) | gitleaks, secret scanning | a credential may have leaked |
| [postmortem-template](postmortem-template.md) | Datadog monitor "slow mean time to resolve" | writing the postmortem of an incident |

Finished postmortems live in [`docs/postmortems/`](../postmortems).
