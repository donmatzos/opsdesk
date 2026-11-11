# MCP token rotation

Use this to rotate the bearer token that protects the MCP endpoint `/mcp`, on schedule or after a suspected leak.

## Symptoms
- MCP clients get 401 after a rotation until they use the new token.
- 503 on `/mcp` means no token is configured (`OPSDESK_MCP_TOKEN` unset): the endpoint is disabled on purpose.

## First checks
- Which Secret holds the token? The Secret named in `mcp.tokenSecret` (key `token`), created outside the chart.
- Confirm the pods use it: `oc set env deployment/opsdesk --list` shows `OPSDESK_MCP_TOKEN` as a reference to the Secret, never the value.
- Leak suspected: look in Kibana for requests to `/mcp` with 401 or unknown clients.

## Mitigation
- Generate a new random token locally (`openssl rand -hex 32`) and store it in your password manager.
- Update the Secret with `oc create secret generic <name> --from-literal=token=<new> --dry-run=client -o yaml | oc apply -f -`; keep the value out of shell history.
- Restart so the pods read it: `oc rollout restart deployment/opsdesk` and wait with `oc rollout status deployment/opsdesk`.
- Give the new token to the MCP clients and verify with `tools/mcp-verify.sh`; the old token must now answer 401.
- Whitespace around the token is stripped on both sides, but a trailing newline in the Secret is a common mistake; use `--from-literal`.

## Escalation
- Token used by an unknown client: treat as a security incident and inform the security contact.

## Related
- `pod-not-ready`
