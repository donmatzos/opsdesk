# Pod not ready

Use this when opsdesk pods are `0/1 Ready`, in `CrashLoopBackOff`, or a deploy hangs on `oc rollout status`.

## Symptoms
- The readiness probe is failing after a deploy: `/readyz` does not return 200.
- `CrashLoopBackOff`, `ImagePullBackOff` or `CreateContainerConfigError` in `oc get pods`.
- The rollout never finishes and the old pods keep serving.

## First checks
- `oc describe pod <pod>`: look at Events (image pull, missing Secret, probe failures, OOMKilled).
- `oc logs <pod> --previous` for the reason of the last crash; the startup fails fast when `SPRING_DATASOURCE_*` is missing.
- `CreateContainerConfigError`: a Secret named in the chart values (`database.existingSecret`, `mcp.tokenSecret`) does not exist.
- `/livez` ok but `/readyz` failing means the app runs but a dependency (database) is not reachable: see `database-unavailable`.
- OpenShift runs the image with a random UID; a file written outside `/tmp` fails with a permission error.

## Mitigation
- Bad release: `helm rollback opsdesk <revision>`.
- Missing Secret: create it, then `oc rollout restart deployment/opsdesk`.
- OOMKilled: raise the memory limit in the values and redeploy.

## Escalation
- No pod ready for more than 10 minutes in production: open SEV1 (`sev1-incident`).

## Related
- `database-unavailable`, `http-errors`, `mcp-token-rotation`
