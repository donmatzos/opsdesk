# Status (current state and next step only)
Last updated: 2026-11-10 (phase 5 and CI change merged).

## Where we are
Phase 1 merged (PR #1, #2): one `opsdesk.jar` (Angular + Spring Boot), catalog + incident API on PostgreSQL, 41 TestNG/Mockito/Testcontainers tests, 2 Vitest tests. History note: the branch history was rewritten on 2026-11-10 to remove a committed local DB password; `main` still has the old phase-1 plan text naming it.
Phase 2 merged (PR #3): JaCoCo + Vitest coverage, SonarCloud, `Dockerfile` (UBI JRE, UID 1001/GID 0, random-UID + read-only verified), compose profile `app`, `tools/smoke-test.sh`, `ci.yml` + reusable `maven-verify.yml` (gitleaks, Trivy fs/image with SARIF, smoke test, GHCR publish gated by `PUBLISH_IMAGE`), Dependabot, pre-commit gitleaks.

Phase 3 merged (PR #7): probes + graceful shutdown; Helm chart (restricted SCC, Route/Ingress, NetworkPolicies, values schema, 25 helm-unittest tests, kubeconform, Trivy via `trivy.yaml`), dev PostgreSQL (sclorg) with in-cluster password, prod values (mock), `tools/kind-verify.sh` (passed locally), CI jobs `helm` and `deploy-dev` (OpenShift, off until `OPENSHIFT_DEPLOY=true`).

Phase 4 merged (PR #8): incident metrics (`/actuator/prometheus`, after-commit), ECS JSON logs with trace ids, local Elastic stack (`tools/elastic-verify.sh`, passed), Datadog mock (profile + key check, chart tagging), Terraform monitors/dashboard (CI job `terraform`), Dependabot: terraform, docker-compose, TypeScript majors ignored.

Phase 5 merged (PR #10): MCP server in the backend (`/mcp`, stateless Streamable HTTP, bearer token `OPSDESK_MCP_TOKEN` stripped of whitespace, 503 when unset, 401 when wrong), 6 tools, Helm `mcp.tokenSecret`, root `.mcp.json`, `tools/mcp-verify.sh` (passed locally). The `mcp/` module is gone: MCP lives in the backend. The Claude Desktop setup in `mcp.md` is untested. Details: `docs/context/mcp.md`.

CI change merged (PR #11): push and PR run only `verify` (build + Sonar); context check, gitleaks, Trivy, Terraform, Helm, image, publish and deploy run only for pushes to `main`. Gitleaks therefore no longer runs on PRs: rely on the local pre-commit hook.

## Next up
1. Phase 6 `feature/runbook-rag`: plan `docs/plans/2026-11-10-phase-6-runbook-rag.md` (owner review and its 2 open questions pending).
2. Plans for phases 7 and 8 are written (`docs/plans/2026-11-10-phase-{7,8}-*.md`, owner review pending, open questions inside); phase 7 starts with a UI/UX analysis.
