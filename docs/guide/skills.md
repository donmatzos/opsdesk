# What this project shows

For each skill area: what exists in the repository and where to look. "Mocked" means the code is real but runs against a fake or is switched off, because there is no cloud account.

## Operations
| Skill | What it shows | Where |
|---|---|---|
| GitHub Actions | Build and test on every push; on `main` secret scan, Trivy, Helm and Terraform checks, image build, optional publish and deploy | [ci.yml](../../.github/workflows/ci.yml), [maven-verify.yml](../../.github/workflows/maven-verify.yml) |
| Docker | Multi-stage image, non-root, arbitrary user id, read-only root filesystem verified | [Dockerfile](../../Dockerfile), [tools/ci-smoke.sh](../../tools/ci-smoke.sh) |
| Kubernetes, OpenShift, Helm | Chart with probes, Route and Ingress switch, NetworkPolicy, restricted security context, values per stage; checked with helm-unittest, kubeconform and a local kind cluster | [deploy/helm/opsdesk](../../deploy/helm/opsdesk), [tools/kind-verify.sh](../../tools/kind-verify.sh) |
| DevSecOps | gitleaks, Trivy (files and image, SARIF to code scanning), SonarCloud, Dependabot, secrets only from the environment | [ci.yml](../../.github/workflows/ci.yml), [Dependabot](../../.github/dependabot.yml) |
| Observability | Business metrics, Prometheus endpoint, JSON logs with trace ids, a local Elastic stack; Datadog parts are mocked | [IncidentMetrics](../../backend/src/main/java/at/donmatzos/opsdesk/metrics/IncidentMetrics.java), [tools/elastic-verify.sh](../../tools/elastic-verify.sh) |
| CI templating | One shared GitLab pipeline template configured only by variables, a consumer that contains no jobs, and the reusable GitHub workflow; same stages and gates as the real pipeline (GitLab mocked, validated in CI) | [ci/gitlab](../../ci/gitlab), [maven-verify.yml](../../.github/workflows/maven-verify.yml) |
| Jenkins | A declarative Jenkinsfile with the same stages, credential ids only, parsed in CI (mocked) | [Jenkinsfile](../../ci/jenkins/Jenkinsfile) |
| Terraform | Datadog monitors and dashboard, Azure OpenAI and AI Search resources, AWX job templates; formatted, validated and linted in CI, never applied (mocked) | [datadog](../../infra/terraform/datadog), [azure](../../infra/terraform/azure), [awx](../../infra/terraform/awx) |
| Ansible Tower / AWX | A Helm deploy playbook (atomic upgrade, smoke test, rollback) linted with the production profile, and the job templates that run it (mocked) | [playbook](../../infra/ansible/playbooks/deploy.yml), [awx](../../infra/terraform/awx) |
| Artifactory | A Maven distribution profile and publish steps in the GitLab and Jenkins pipelines; real artifacts go to GHCR and GitHub Releases (mocked) | [pom.xml](../../pom.xml) |
| Releases | A tag builds the jar, image and Helm chart, gates on Trivy and the smoke test, publishes to GHCR and a GitHub release | [release.yml](../../.github/workflows/release.yml), [release-check.sh](../../tools/release-check.sh) |
| Incident analysis | Runbooks for the alerts the monitors raise, a postmortem template and a real blameless postmortem; incidents are the domain of the app | [runbooks](../runbooks), [postmortems](../postmortems) |
| Bash, Python, Linux | Smoke and verification scripts, context and contrast checks | [tools](../../tools) |

## Software engineering
| Skill | What it shows | Where |
|---|---|---|
| Java 25, Spring Boot | REST API, validation, problem details, records, graceful shutdown, probes | [backend](../../backend/src/main/java/at/donmatzos/opsdesk) |
| PostgreSQL, Flyway | Migrations, Spring Data JDBC, real database in tests | [migrations](../../backend/src/main/resources/db/migration) |
| Maven multi-module | One runnable jar with the Angular build inside | [pom.xml](../../pom.xml) |
| Angular, TypeScript | Standalone components, signals, accessible UI (WCAG 2.2 AA), this guide | [frontend](../../frontend/src/app), [design](design.md) |
| TestNG, Mockito, Vitest | Unit, slice and full-stack tests; coverage with JaCoCo and Vitest | [testing notes](../context/testing.md) |

## AI and LLM
| Skill | What it shows | Where |
|---|---|---|
| MCP server | Stateless Streamable HTTP, bearer token, seven tools over services, incidents and runbooks | [mcp.md](../context/mcp.md), [tools/mcp-verify.sh](../../tools/mcp-verify.sh) |
| RAG | BM25 retrieval over the runbooks, grounded answers with sources, no model call without a match | [ai module](../../ai/src/main/java/at/donmatzos/opsdesk/ai) |
| Azure OpenAI, Azure AI Search | REST adapters behind a Spring profile, fail fast without configuration, tested against a local fake server (mocked) | [AzureAiConfig](../../backend/src/main/java/at/donmatzos/opsdesk/runbook/AzureAiConfig.java) |
| Agentic engineering | A small context system for coding agents (one entry file, size-checked context files), a project skill and the agent setup as code with a pinned lockfile | [CLAUDE.md](../../CLAUDE.md), [ops-check](../../.claude/skills/ops-check/SKILL.md), [apm.yml](../../apm.yml) |
