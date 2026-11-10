# Status (current state and next step only)
Last updated: 2026-11-10.

## Where we are
Phase 1 merged (PR #1, #2): one `opsdesk.jar` (Angular + Spring Boot), catalog + incident API on PostgreSQL, 41 TestNG/Mockito/Testcontainers tests, 2 Vitest tests. History note: the branch history was rewritten on 2026-11-10 to remove a committed local DB password; `main` still has the old phase-1 plan text naming it.
Phase 2 merged (PR #3): JaCoCo + Vitest coverage, SonarCloud, `Dockerfile` (UBI JRE, UID 1001/GID 0, random-UID + read-only verified), compose profile `app`, `tools/smoke-test.sh`, `ci.yml` + reusable `maven-verify.yml` (gitleaks, Trivy fs/image with SARIF, smoke test, GHCR publish gated by `PUBLISH_IMAGE`), Dependabot, pre-commit gitleaks.

## Next up
1. Phase 3 `feature/openshift-deploy`: plan `docs/plans/2026-11-10-phase-3-openshift-deploy.md` (owner review pending). Real deploy needs an OpenShift namespace (Developer Sandbox) + `OPENSHIFT_*` secrets/variables + `PUBLISH_IMAGE=true`.
