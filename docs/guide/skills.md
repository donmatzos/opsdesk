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
| Terraform | Datadog monitors and dashboard, Azure OpenAI and AI Search resources; formatted, validated and linted in CI, never applied (mocked) | [datadog](../../infra/terraform/datadog), [azure](../../infra/terraform/azure) |
| Incident analysis | Runbooks for the alerts the monitors raise; incidents are the domain of the app | [docs/runbooks](../runbooks) |
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
| Agentic engineering | A small context system for coding agents: one entry file, size-checked context files | [CLAUDE.md](../../CLAUDE.md), [context](../context) |

## Planned
GitLab CI shared template and consumer pipeline, a Jenkinsfile and an Ansible playbook with a Tower job template (all mocked), and a postmortem template. They are not in the repository yet.
