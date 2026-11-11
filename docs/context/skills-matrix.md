# Skills matrix (resume -> project)
Source: owner's resume (2026-11). Direction: DevOps with a software engineering background. Priority: A = must be real and visible, B = real but small, C = mocked/commented, D = mention only.

## Ops (the main story)
| Skill | Pri | Where it shows |
|---|---|---|
| GitHub Actions | A | `.github/workflows/`: build, test, scans, image, release; real and green |
| CI templating (master thesis!) | A | `ci/gitlab/` (done, mock): shared pipeline template + a consumer `.gitlab-ci.yml` configured only by variables, schema + job set checked in CI; also reusable GH workflow (`workflow_call`, real) |
| Docker | A | multi-stage `Dockerfile`, non-root, arbitrary UID (OpenShift-safe), healthcheck |
| Kubernetes / OpenShift, Helm | A | `deploy/helm/opsdesk` (Deployment, Service, Route/Ingress switch, probes, NetworkPolicy, values per stage); CI: `helm lint/template` always; `oc login` + `helm upgrade --install` + smoke test, gated on OpenShift secrets (off without a cluster; Red Hat Developer Sandbox can make it real) |
| DevSecOps: Trivy, SonarQube, secrets | A | Trivy fs + image scan in CI; SonarCloud (free for public repos) or Sonar step mocked; secrets only via CI secrets; Dependabot |
| Observability: Datadog, Elastic | A | Micrometer business metrics + `/actuator/prometheus` (real); ECS JSON logs with trace ids + local Elastic stack (Filebeat, Kibana) (real); Datadog registry, Unified Service Tagging, monitors/dashboard as Terraform (C) |
| Terraform (IaC) | B | `infra/terraform/datadog` (monitors, dashboard; fmt/validate/tflint in CI, done) and `azure` (done, validate only); `apply` never runs |
| Ansible Tower | C | done (mock): `infra/ansible/playbooks/deploy.yml` (ansible-lint production profile), job templates in `infra/terraform/awx` (validate only) |
| Jenkins | C | done (mock): `ci/jenkins/Jenkinsfile`, same stages as GH Actions, parsed in CI, credential ids only |
| Artifactory | C | done (mock): Maven profile `artifactory`, publish steps in the GitLab template and the Jenkinsfile; real publish to GHCR / GitHub Releases |
| Release + deploy per stage | B | done: `release.yml` (tag -> jar, GHCR image, chart, GitHub release; first tag pending), values files `dev`/`prod`, rollback runbook |
| Incident analysis | B | done: `docs/runbooks/` (7 runbooks + postmortem template), `docs/postmortems/` (one real, blameless); app has incidents as domain |
| Linux, Bash, Python, networking | B | `tools/` scripts (Bash smoke test, Python context check); NetworkPolicy, probes |

## Software engineering
| Skill | Pri | Where it shows |
|---|---|---|
| Java 25, Spring Boot | A | `backend`: REST API, validation, error handling (ProblemDetail), records |
| PostgreSQL, Flyway | B | Spring Data JDBC, migrations, Testcontainers; Postgres in compose and Helm |
| Maven multi-module, frontend-maven-plugin | A | parent POM, one runnable jar with Angular inside |
| Angular, TypeScript | A | done (phase 7): dashboard, ask-the-runbooks, project guide, WCAG 2.2 AA checked with axe in CI, static demo on GitHub Pages with an in-browser API mock |
| TestNG, Mockito | A | unit + Spring slice tests in TestNG; coverage via JaCoCo |
| Service extraction / microservice | D | README note on module boundaries (modules could be split later) |
| Gradle, Groovy, XML, YAML | D | Groovy via Jenkinsfile; rest implicit |

## AI / LLM (the differentiator)
| Skill | Pri | Where it shows |
|---|---|---|
| MCP server development | A | done (phase 5): MCP server in the backend (`/mcp`, bearer token, 6 tools over services and incidents; `search_runbooks` in phase 6), `.mcp.json` for Claude Code, `tools/mcp-verify.sh` |
| RAG | B | done (phase 6): BM25 retrieval over `docs/runbooks/` (real), grounded answers, `/api/assist` + MCP `search_runbooks`; answer composing behind `LlmClient` (mock) |
| Azure OpenAI, Azure AI Search | C | done (mock): REST adapters tested against a fake server, profile `azure` (off, fails fast), Terraform validated in CI |
| Agentic engineering, context engineering | A | this context system (`CLAUDE.md` router, small files, size check), `AGENTS.md` for Copilot/other agents, `.claude/skills/`, optional `apm.yml` |
| TDD / subagent-driven dev | D | visible in commit history and plan docs, not in code |

## Not shown on purpose
Scrum (process), German/English, the automotive domain (employer's, not reused).

## Phases
See `docs/plans/2026-11-10-roadmap.md`.
