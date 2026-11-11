# Status (current state and next step only)
Last updated: 2026-11-11 (phase 8 implemented on `update/templates-and-polish`, PR pending).

## Where we are
Phase 1 merged (PR #1, #2): one `opsdesk.jar` (Angular + Spring Boot), catalog + incident API on PostgreSQL, 41 TestNG/Mockito/Testcontainers tests, 2 Vitest tests. History note: the branch history was rewritten on 2026-11-10 to remove a committed local DB password; `main` still has the old phase-1 plan text naming it.
Phase 2 merged (PR #3): JaCoCo + Vitest coverage, SonarCloud, `Dockerfile` (UBI JRE, UID 1001/GID 0, random-UID + read-only verified), compose profile `app`, `tools/smoke-test.sh`, `ci.yml` + reusable `maven-verify.yml` (gitleaks, Trivy fs/image with SARIF, smoke test, GHCR publish gated by `PUBLISH_IMAGE`), Dependabot, pre-commit gitleaks.

Phase 3 merged (PR #7): probes + graceful shutdown; Helm chart (restricted SCC, Route/Ingress, NetworkPolicies, values schema, 25 helm-unittest tests, kubeconform, Trivy via `trivy.yaml`), dev PostgreSQL (sclorg) with in-cluster password, prod values (mock), `tools/kind-verify.sh` (passed locally), CI jobs `helm` and `deploy-dev` (OpenShift, off until `OPENSHIFT_DEPLOY=true`).

Phase 4 merged (PR #8): incident metrics (`/actuator/prometheus`, after-commit), ECS JSON logs with trace ids, local Elastic stack (`tools/elastic-verify.sh`, passed), Datadog mock (profile + key check, chart tagging), Terraform monitors/dashboard (CI job `terraform`), Dependabot: terraform, docker-compose, TypeScript majors ignored.

Phase 5 merged (PR #10): MCP server in the backend (`/mcp`, stateless Streamable HTTP, bearer token `OPSDESK_MCP_TOKEN` stripped of whitespace, 503 when unset, 401 when wrong), 6 tools, Helm `mcp.tokenSecret`, root `.mcp.json`, `tools/mcp-verify.sh` (passed locally). The `mcp/` module is gone: MCP lives in the backend. The Claude Desktop setup in `mcp.md` is untested. Details: `docs/context/mcp.md`.

CI change merged (PR #11): push and PR run only `verify` (build + Sonar); context check, gitleaks, Trivy, Terraform, Helm, image, publish and deploy run only for pushes to `main`. Gitleaks therefore no longer runs on PRs: rely on the local pre-commit hook.

Phase 6 merged (PR #13): new `ai` module (plain Java: runbook parser, BM25 index, `RunbookAssistant`, `MockLlmClient`, Azure OpenAI/AI Search REST adapters), 5 runbooks packaged into the jar, `POST /api/assist`, MCP tool `search_runbooks` (7 tools), profile `azure` (off, fails fast, tested against a fake server only), `infra/terraform/azure` (validate only, CI matrix). Local LLM decision: mock only. Checked locally: `./mvnw verify`, `tools/smoke-test.sh`, `tools/mcp-verify.sh`, terraform fmt/validate/tflint in Docker.

Sonar cleanup merged (PR #15). `/api/assist` is open and unthrottled on purpose: close/throttle it before real use with profile `azure`.

Phase 7 merged (PR #16): accessible Angular UI, project guide with 31 sourced glossary terms, static demo with an in-browser API mock, `pages.yml` (build, axe check, deploy gated by `PAGES_DEPLOY`). Three docs.redhat.com glossary links answer 403 to bots and are unverified.

UI rework merged (PR #17, Sonar fixes PR #18): palette Indigo, System/Light/Dark toggle, sticky shell, Overview page, list-first incidents, formatted assist answers; layout regressions are guarded by `tools/pages-a11y.sh`. Details: `docs/guide/design.md`.

Phase 8 implemented (branch `update/templates-and-polish`, not merged): Artifactory Maven profile, GitLab shared template + consumer, Jenkinsfile, Ansible deploy playbook, AWX job templates (Terraform), `release.yml` + `tools/release-check.sh`, runbooks (`deploy-rollback`, `secret-exposure`, postmortem template) and a blameless postmortem, `ops-check` skill + `apm.yml` with a pinned lockfile, README with architecture diagram, skills table and mock inventory, `tools/docs_check.py`. All mocks are validated by CI job `templates` and checked locally (`tools/ci-templates-check.sh all`, actionlint, terraform fmt/validate/tflint, `./mvnw verify`). Not checked: the release workflow itself (no tag yet), GitLab/Jenkins/AWX runs, `apm install` in this repo (validated in a scratch copy), the live Pages deploy.

## Next up
1. Merge phase 8. Re-run the `pages` workflow (Actions -> pages -> Run workflow) or merge to `main`: the earlier runs predate `PAGES_DEPLOY=true`, so the demo is not live yet; the README links its URL.
2. First release: a version PR sets `0.1.0` in `pom.xml` and the chart (already `0.1.0`), the owner tags `v0.1.0`; the next PR bumps the pom to the next SNAPSHOT.
3. Open: `/api/assist` is open and unthrottled (close/throttle before real Azure use).
