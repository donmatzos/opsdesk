# Glossary

Terms used in opsdesk and its operations, each with a short definition and a link to an authoritative source. Where a term has a counterpart in the repository, a link points to it.

## Arbitrary UID

OpenShift starts containers with a random user id from a namespace range instead of the id in the image, so files the app writes must be usable by group 0 and the image must not depend on a fixed user.

Source: [OpenShift guidelines for creating images](https://docs.redhat.com/en/documentation/openshift_container_platform/4.17/html/images/creating-images)

In this repo: [Dockerfile](../../Dockerfile)

## BM25

A ranking function that scores a document for a query from term frequency, rarity of the term and document length. opsdesk uses it, with headings counting double, to find runbook sections.

Source: [Robertson and Zaragoza, The Probabilistic Relevance Framework: BM25 and Beyond](https://www.nowpublishers.com/article/Details/INR-019)

In this repo: [RunbookIndex](../../ai/src/main/java/at/donmatzos/opsdesk/ai/search/RunbookIndex.java)

## CI/CD

Continuous integration builds and tests every change automatically; continuous delivery (or deployment) ships passing builds to an environment with little manual work.

Source: [Atlassian, CI vs CD vs deployment](https://www.atlassian.com/continuous-delivery/principles/continuous-integration-vs-delivery-vs-deployment)

In this repo: [ci.yml](../../.github/workflows/ci.yml)

## Datadog

A hosted monitoring service for metrics, logs and traces. opsdesk can export metrics to it and defines monitors as code, but no Datadog account is used, so this part is mocked.

Source: [Datadog documentation: Getting started](https://docs.datadoghq.com/getting_started/)

In this repo: [Terraform for Datadog](../../infra/terraform/datadog)

## ECS logging

The Elastic Common Schema is a shared set of field names for logs (for example `trace.id`), so logs from different services can be searched and correlated the same way.

Source: [Elastic Common Schema reference](https://www.elastic.co/guide/en/ecs/current/ecs-reference.html)

In this repo: [application.yml](../../backend/src/main/resources/application.yml)

## Error budget

The amount of unreliability a service may have in a period, derived from its SLO (a 99.9 % SLO leaves 0.1 %). While budget is left, teams can ship; when it is used up, reliability work comes first.

Source: [Google SRE Workbook: Error budget policy](https://sre.google/workbook/error-budget-policy/)

## Flyway

A tool that applies versioned SQL migrations to a database in order and records which ones ran, so every environment gets the same schema.

Source: [Flyway documentation](https://documentation.red-gate.com/flyway)

In this repo: [migrations](../../backend/src/main/resources/db/migration)

## gitleaks

A scanner that looks for secrets such as keys and passwords in files and git history. It runs as a pre-commit hook and over the full history in CI.

Source: [gitleaks on GitHub](https://github.com/gitleaks/gitleaks)

In this repo: [ci.yml](../../.github/workflows/ci.yml)

## Helm

The package manager for Kubernetes: a chart bundles templates and default values, and `helm upgrade --install` renders them into manifests and keeps a revision history for rollbacks.

Source: [Helm documentation: Using Helm](https://helm.sh/docs/intro/using_helm/)

In this repo: [opsdesk chart](../../deploy/helm/opsdesk)

## Helm chart

A directory with `Chart.yaml`, templates and `values.yaml` that describes one application for Kubernetes. Stages differ only by values files.

Source: [Helm documentation: Charts](https://helm.sh/docs/topics/charts/)

In this repo: [Chart.yaml](../../deploy/helm/opsdesk/Chart.yaml)

## Incident severity

A label for how bad an incident is, which decides who is woken up and how fast. opsdesk uses SEV1 (most severe) to SEV3.

Source: [PagerDuty incident response: Severity levels](https://response.pagerduty.com/before/severity_levels/)

In this repo: [Severity](../../backend/src/main/java/at/donmatzos/opsdesk/incident/Severity.java)

## Ingress

A Kubernetes object that routes HTTP traffic from outside the cluster to Services. The chart uses it on plain Kubernetes; on OpenShift it uses a Route.

Source: [Kubernetes documentation: Ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/)

In this repo: [ingress template](../../deploy/helm/opsdesk/templates/ingress.yaml)

## Liveness/readiness probe

Checks the platform runs against a container. A failing liveness probe restarts it; a failing readiness probe takes it out of the load balancer without restarting. opsdesk serves `/livez` and `/readyz`.

Source: [Kubernetes documentation: Configure liveness, readiness and startup probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)

In this repo: [deployment template](../../deploy/helm/opsdesk/templates/deployment.yaml)

## LLM

A large language model: a neural network trained on large amounts of text that generates text from a prompt. In opsdesk the model only composes an answer from retrieved runbook text, and by default it is a mock.

Source: [Brown et al., Language Models are Few-Shot Learners](https://arxiv.org/abs/2005.14165)

In this repo: [MockLlmClient](../../ai/src/main/java/at/donmatzos/opsdesk/ai/llm/MockLlmClient.java)

## MCP

The Model Context Protocol: an open standard that lets AI applications call tools and read data from other systems over a common interface. opsdesk is an MCP server.

Source: [Model Context Protocol documentation](https://modelcontextprotocol.io/introduction)

In this repo: [mcp.md](../context/mcp.md)

## Micrometer

A metrics facade for JVM applications: code records counters, timers and gauges once, and a registry exports them to Prometheus, Datadog and others.

Source: [Micrometer documentation](https://docs.micrometer.io/micrometer/reference/)

In this repo: [IncidentMetrics](../../backend/src/main/java/at/donmatzos/opsdesk/metrics/IncidentMetrics.java)

## MTTR

Mean time to resolve (or recover): the average time from the start of incidents to their resolution. It is a trend indicator, not a target for one incident.

Source: [Atlassian, Common incident management metrics](https://www.atlassian.com/incident-management/kpis/common-metrics)

In this repo: [IncidentMetrics](../../backend/src/main/java/at/donmatzos/opsdesk/metrics/IncidentMetrics.java)

## NetworkPolicy

A Kubernetes object that limits which pods may talk to each other. The chart allows only the traffic the application needs.

Source: [Kubernetes documentation: Network Policies](https://kubernetes.io/docs/concepts/services-networking/network-policies/)

In this repo: [networkpolicy template](../../deploy/helm/opsdesk/templates/networkpolicy.yaml)

## Postmortem

A written review of an incident: what happened, the impact, the causes and the follow-up actions, written without blaming people.

Source: [Google SRE book: Postmortem culture](https://sre.google/sre-book/postmortem-culture/)

## Prometheus

A monitoring system that scrapes metrics from HTTP endpoints and stores them as time series. opsdesk exposes `/actuator/prometheus` on a separate management port.

Source: [Prometheus documentation: Overview](https://prometheus.io/docs/introduction/overview/)

In this repo: [application.yml](../../backend/src/main/resources/application.yml)

## RAG

Retrieval-augmented generation: find relevant text first, then let a language model answer from it. This keeps answers tied to known documents and makes them citable.

Source: [Lewis et al., Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks](https://arxiv.org/abs/2005.11401)

In this repo: [RunbookAssistant](../../ai/src/main/java/at/donmatzos/opsdesk/ai/RunbookAssistant.java)

## Reusable workflow

A GitHub Actions workflow that other workflows call with `workflow_call`, so build logic lives in one place.

Source: [GitHub Docs: Reusing workflows](https://docs.github.com/en/actions/sharing-automations/reusing-workflows)

In this repo: [maven-verify.yml](../../.github/workflows/maven-verify.yml)

## Route

The OpenShift object that exposes a Service on a host name, similar to an Ingress. The chart switches between Route and Ingress.

Source: [OpenShift documentation: Configuring Routes](https://docs.redhat.com/en/documentation/openshift_container_platform/4.17/html/ingress_and_load_balancing/configuring-routes)

In this repo: [route template](../../deploy/helm/opsdesk/templates/route.yaml)

## Runbook

A short, tested procedure for handling a known alert or failure: symptoms, first checks, mitigation, escalation. opsdesk's assistant answers from its runbooks.

Source: [Google SRE book: Being on-call](https://sre.google/sre-book/being-on-call/)

In this repo: [runbooks](../runbooks)

## SARIF

The Static Analysis Results Interchange Format: a JSON format for findings of analysis tools. CI uploads Trivy results as SARIF so GitHub shows them in code scanning.

Source: [OASIS SARIF 2.1.0 specification](https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html)

In this repo: [ci.yml](../../.github/workflows/ci.yml)

## SCC

Security context constraints are OpenShift's rules for what a pod may do (user id, capabilities, volumes). opsdesk is written for the restricted one.

Source: [OpenShift documentation: Managing security context constraints](https://docs.redhat.com/en/documentation/openshift_container_platform/4.17/html/authentication_and_authorization/managing-pod-security-policies)

In this repo: [deployment template](../../deploy/helm/opsdesk/templates/deployment.yaml)

## SLI

A service level indicator is a measured number that describes service quality, for example the share of requests that succeed or the latency at the 99th percentile.

Source: [Google SRE book: Service Level Objectives](https://sre.google/sre-book/service-level-objectives/)

## SLO

A service level objective is the target for an SLI over a period, for example 99.9 % of requests succeed in 30 days. It is the basis of the error budget.

Source: [Google SRE book: Service Level Objectives](https://sre.google/sre-book/service-level-objectives/)

## Terraform

Infrastructure as code: resources are described in files, `plan` shows the changes and `apply` makes them. In this repository the code is validated in CI but never applied.

Source: [Terraform documentation: What is Terraform](https://developer.hashicorp.com/terraform/intro)

In this repo: [infra/terraform](../../infra/terraform)

## Testcontainers

A library that starts real services such as PostgreSQL in throwaway containers for tests, so tests run against the same database as production.

Source: [Testcontainers for Java](https://java.testcontainers.org/)

In this repo: [TestcontainersConfig](../../backend/src/test/java/at/donmatzos/opsdesk/TestcontainersConfig.java)

## Trivy

A scanner for vulnerabilities, secrets and misconfigurations in files, container images and Helm charts. CI fails on HIGH and CRITICAL findings that have a fix.

Source: [Trivy documentation](https://trivy.dev/latest/docs/)

In this repo: [trivy.yaml](../../trivy.yaml)
