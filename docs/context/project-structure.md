# Project structure (where things live, where new things go)
| Path | Content |
|---|---|
| `pom.xml` | parent POM: Spring Boot parent, Java 25, modules `frontend`, `ai`, `backend`, versions; profile `artifactory` (mock) |
| `backend/` | Spring Boot app, packages `at.donmatzos.opsdesk.{catalog,incident,runbook,metrics,mcp,common,config}` (`mcp` = MCP tools + bearer token filter) |
| `ai/` | RAG + LLM/search adapters, `at.donmatzos.opsdesk.ai.{search,llm}`; packages `docs/runbooks/*.md` into the jar (`runbooks/`) |
| `frontend/` | Angular workspace (`src/app/<feature>/`), `pom.xml` runs npm via frontend-maven-plugin |
| `Dockerfile` | multi-stage image (root): non-root, arbitrary UID, read-only root verified |
| `deploy/helm/opsdesk/` | Helm chart: `templates/`, `tests/` (helm-unittest), `values.yaml` + `values.schema.json`, `values-dev.yaml`, `values-prod.yaml` (mock) |
| `deploy/openshift/` | `README.md`: Developer Sandbox, CI service account, secrets, manual deploy/rollback |
| `deploy/compose/` | local `compose.yml` (app, optional Elastic/Postgres) |
| `infra/terraform/datadog/` | monitors, dashboard as code (validate only) |
| `infra/terraform/azure/` | AI Search + OpenAI resources (validate only, commented backend), `search-index.json` |
| `infra/terraform/awx/` | AWX project, inventories and job templates per stage (validate only) |
| `infra/ansible/` | `playbooks/deploy.yml` (Helm atomic upgrade, smoke test, rollback), inventories, `.ansible-lint` (mocked, AWX runs it) |
| `ci/gitlab/` | `templates/` shared pipeline (same stages as `ci.yml`), `consumer/` variables-only example, README (mocked) |
| `ci/jenkins/` | `Jenkinsfile` (mocked) |
| `.github/workflows/` | real CI/CD: `ci.yml`, `maven-verify.yml` (reusable), `release.yml`, `pages.yml` |
| `docs/context/` | agent context (this) · `docs/runbooks/` runbooks (also RAG input; `README.md` index, `*-template.md` skipped by the loader) · `docs/postmortems/` blameless postmortems · `docs/adr/` decisions · `docs/plans/` roadmap + one plan per phase |
| `docs/guide/` | public project guide (Markdown + SVG), shown in the app and on GitHub; `design.md` = binding UI rules |
| `tools/` | `context_check.py`, `contrast_check.py`, `guide_check.py`, `docs_check.py`, `ci-templates-check.sh`, `release-check.sh`, `smoke-test.sh`, `ci-smoke.sh`, `helm-check.sh`, `kind-verify.sh`, `elastic-verify.sh`, `mcp-verify.sh`, `pages-a11y.sh` (+ `.mjs`) |
| `.claude/skills/` | project skills for agents: `ops-check`; `apm.yml` + `apm.lock.yaml` at the root describe the agent setup |

New domain feature -> own package in `backend` + test in the same package under `src/test`. Schema change -> new `backend/src/main/resources/db/migration/V<n>__<name>.sql` (never edit an applied one); demo rows -> `db/demo/R__demo_data.sql`. `common` = shared exceptions + `ApiExceptionHandler`; `config` = beans, SPA forwarding. New ops artefact -> `deploy/` (runtime) or `infra/` (provisioning) or `ci/` (pipelines).
