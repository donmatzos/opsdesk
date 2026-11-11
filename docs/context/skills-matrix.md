# Skills matrix (resume -> project)
Source: owner's resume (2026-11). Direction: DevOps with a software engineering background. Priority: A = must be real and visible, B = real but small, C = mocked/commented, D = mention only.

## Ops (the main story)
| Skill | Pri | Where it shows |
|---|---|---|
| GitHub Actions | A | `.github/workflows/`: build, test, scans, image, release; real and green |
| CI templating (master thesis!) | A | `ci/gitlab/`: shared, reusable pipeline template + a consumer `.gitlab-ci.yml` configured only by variables; also reusable GH workflow (`workflow_call`) |
| Docker | A | multi-stage `Dockerfile`, non-root, arbitrary UID (OpenShift-safe), healthcheck |
| Kubernetes / OpenShift, Helm | A | `deploy/helm/opsdesk` (Deployment, Service, Route/Ingress switch, probes, NetworkPolicy, values per stage); CI: `helm lint/template` always; `oc login` + `helm upgrade --install` + smoke test, gated on OpenShift secrets (off without a cluster; Red Hat Developer Sandbox can make it real) |
| DevSecOps: Trivy, SonarQube, secrets | A | Trivy fs + image scan in CI; SonarCloud (free for public repos) or Sonar step mocked; secrets only via CI secrets; Dependabot |
| Observability: Datadog, Elastic | A | Micrometer business metrics + `/actuator/prometheus` (real); ECS JSON logs with trace ids + local Elastic stack (Filebeat, Kibana) (real); Datadog registry, Unified Service Tagging, monitors/dashboard as Terraform (C) |
| Terraform (IaC) | B | `infra/terraform/datadog` (monitors, dashboard; fmt/validate/tflint in CI, done) and `azure` (done, validate only); `apply` never runs |
| Ansible Tower | C | `infra/ansible`: small playbook + Tower job template via Terraform `awx` provider (commented) |
| Jenkins | C | `ci/jenkins/Jenkinsfile` (Groovy declarative), same stages as GH Actions |
| Artifactory | C | Maven `distributionManagement` + Helm repo push, commented; real publish to GitHub Packages / GHCR |
| Release + deploy per stage | B | tags -> GHCR image + Helm chart; values files `dev`/`prod` |
| Incident analysis | B | `docs/runbooks/` (runbook + postmortem template); app has incidents as domain |
| Linux, Bash, Python, networking | B | `tools/` scripts (Bash smoke test, Python context check); NetworkPolicy, probes |

## Software engineering
| Skill | Pri | Where it shows |
|---|---|---|
| Java 25, Spring Boot | A | `backend`: REST API, validation, error handling (ProblemDetail), records |
| PostgreSQL, Flyway | B | Spring Data JDBC, migrations, Testcontainers; Postgres in compose and Helm |
| Maven multi-module, frontend-maven-plugin | A | parent POM, one runnable jar with Angular inside |
| Angular, TypeScript | A | `frontend`: dashboard (services, incidents), standalone components, signals |
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
