# Project structure (where things live, where new things go)
| Path | Content |
|---|---|
| `pom.xml` | parent POM: Spring Boot parent, Java 25, modules `frontend` then `backend`, versions |
| `backend/` | Spring Boot app, packages `at.donmatzos.opsdesk.{catalog,incident,runbook,metrics,common,config}` |
| `mcp/` | MCP server tools, package `at.donmatzos.opsdesk.mcp` |
| `ai/` | RAG + LLM/search adapters, `at.donmatzos.opsdesk.ai.{search,llm}` |
| `frontend/` | Angular workspace (`src/app/<feature>/`), `pom.xml` runs npm via frontend-maven-plugin |
| `Dockerfile` | multi-stage image (to create, root) |
| `deploy/helm/opsdesk/` | Helm chart; `values-dev.yaml`, `values-prod.yaml` |
| `deploy/openshift/` | OpenShift-only extras (Route, SCC notes) |
| `deploy/compose/` | local `compose.yml` (app, optional Elastic/Postgres) |
| `infra/terraform/datadog/` | monitors, dashboard as code (validate only) |
| `infra/terraform/azure/` | AI Search + OpenAI resources (validate only, commented backend) |
| `infra/ansible/` | playbook + Tower job template (mocked) |
| `ci/gitlab/` | shared pipeline template + consumer example |
| `ci/jenkins/` | `Jenkinsfile` (mocked) |
| `.github/workflows/` | real CI/CD |
| `docs/context/` | agent context (this) · `docs/runbooks/` runbooks (also RAG input) · `docs/adr/` decisions · `docs/plans/` roadmap + one plan per phase |
| `tools/` | scripts: `context_check.py`, smoke test (Bash) |
| `.claude/skills/` | project skills for agents (optional, open question 9) |

New domain feature -> own package in `backend` + test in the same package under `src/test`. Schema change -> new `backend/src/main/resources/db/migration/V<n>__<name>.sql` (never edit an applied one); demo rows -> `db/demo/R__demo_data.sql`. `common` = shared exceptions + `ApiExceptionHandler`; `config` = beans, SPA forwarding. New ops artefact -> `deploy/` (runtime) or `infra/` (provisioning) or `ci/` (pipelines).
