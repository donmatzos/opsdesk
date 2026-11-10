# Phase 4: Observability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** opsdesk emits business metrics (Prometheus endpoint, Datadog-ready), writes ECS JSON logs with trace ids that a local Elastic stack ingests and Kibana shows, and its Datadog monitors and dashboard exist as validated Terraform.

**Architecture:** `IncidentService` publishes domain events (`IncidentOpened`, `IncidentStateChanged`); an `IncidentMetrics` listener in package `metrics` records them after commit (counter, timer) and exposes active incidents as gauges read from the repository. Micrometer Tracing (OpenTelemetry bridge, no exporter) puts `trace.id`/`span.id` into every log line; Spring Boot's structured logging writes ECS JSON in the container image. A compose profile `observability` runs Elasticsearch + Kibana + Filebeat (reads Docker container log files, keeps only `service.name: opsdesk`). Datadog is a disabled Micrometer registry (profile `datadog`), Unified Service Tagging in the Helm chart (off by default) and Terraform in `infra/terraform/datadog` (validated in CI, never applied).

**Tech Stack:** Micrometer (Boot-managed) + `micrometer-registry-prometheus`, `micrometer-registry-datadog`, Micrometer Tracing OTel bridge; Spring Boot structured logging (ECS); Elasticsearch/Kibana/Filebeat 9.5.5 (`docker.elastic.co`/Docker Hub `elasticsearch:9.5.5`, `kibana:9.5.5`, `elastic/filebeat:9.5.5`); Terraform 1.17.1, Datadog provider 4.30.0, tflint 0.66.0, `hashicorp/setup-terraform` v4.0.1.

**Spec:** `docs/plans/2026-11-10-roadmap.md` (row 4), `docs/context/ops.md` (Observability), `docs/context/cloud-mocks.md` (Datadog row), owner decision 2026-11-10: local Elastic stack as an optional compose profile.

## Global Constraints
- No secrets in the repo, ever: Datadog keys only via env (`DATADOG_API_KEY` for the app, `DD_API_KEY`/`DD_APP_KEY` for Terraform); no defaults, no example values. The local Elastic stack runs with security disabled (`xpack.security.enabled=false`), so it has no password at all; it binds to `127.0.0.1` only.
- Metric names (Micrometer dot notation; Prometheus renders `_` + suffixes): `opsdesk.incidents.opened` (counter, tag `severity`), `opsdesk.incidents.active` (gauge, tags `state` = OPEN|MITIGATED), `opsdesk.incidents.time.to.resolve` (timer, tag `severity`, percentile histogram on). Common tag `application=opsdesk`.
- Metrics are recorded only for committed changes (`@TransactionalEventListener(phase = AFTER_COMMIT)`).
- Actuator exposure stays minimal: `health`, `info`, `prometheus`.
- Datadog parts are marked `MOCK:` with a reason (no account) and are off by default.
- `terraform apply` never runs anywhere; CI runs `fmt -check`, `init -backend=false`, `validate`, `tflint`.
- Branch `feature/observability`; one commit per task; actions pinned by SHA (resolve with `git ls-remote`).

## Review Focus
1. A rolled-back transaction must not count as an opened incident (Task 1).
2. The active-incidents gauge must reflect reality after restarts (read from the DB, not an in-memory counter) (Task 1).
3. Logs from a request carry the same `trace.id` on every line of that request; Kibana finds them by it (Tasks 2, 3).
4. Starting the app with profile `datadog` but no `DATADOG_API_KEY` must fail fast with a clear error, not silently drop metrics (Task 4).
5. The Elastic stack must not expose Elasticsearch/Kibana beyond localhost and must not need any credential (Task 3).

---

### Task 1: Incident business metrics and the Prometheus endpoint

**Files:**
- Create: `backend/src/main/java/at/donmatzos/opsdesk/incident/IncidentOpened.java`, `IncidentStateChanged.java` (records)
- Create: `backend/src/main/java/at/donmatzos/opsdesk/metrics/IncidentMetrics.java`
- Modify: `IncidentService.java` (inject `ApplicationEventPublisher`, publish after save), `IncidentRepository.java` (`long countByState(IncidentState state)`), `backend/pom.xml` (`micrometer-registry-prometheus`), `application.yml` (`management.endpoints.web.exposure.include: health,info,prometheus`, `management.metrics.tags.application: opsdesk`)
- Test: `backend/src/test/java/at/donmatzos/opsdesk/metrics/IncidentMetricsTest.java` (plain TestNG + `SimpleMeterRegistry`, repository mocked with Mockito), `MetricsEndpointTest.java` (extends `IntegrationTest`), `IncidentServiceTest.java` (verify events published)
- Remove: `backend/src/main/java/at/donmatzos/opsdesk/metrics/.gitkeep`

**Interfaces:**
- Produces: `record IncidentOpened(long incidentId, Severity severity)`, `record IncidentStateChanged(long incidentId, Severity severity, IncidentState from, IncidentState to, Instant openedAt, Instant changedAt)`.
- Produces: `@Component class IncidentMetrics` with `IncidentMetrics(MeterRegistry registry, IncidentRepository incidents)` (registers the `active` gauges per state in the constructor), `@TransactionalEventListener(phase = AFTER_COMMIT) void on(IncidentOpened e)` (increment `opsdesk.incidents.opened{severity}`), `@TransactionalEventListener(phase = AFTER_COMMIT) void on(IncidentStateChanged e)` (when `to == RESOLVED`: record `Duration.between(openedAt, changedAt)` in `opsdesk.incidents.time.to.resolve{severity}`).

- [ ] **Step 1: Write the failing tests.**
  - `IncidentMetricsTest`: `shouldCountOpenedIncidentsPerSeverity` (two SEV1 + one SEV3 events -> counter values 2.0 / 1.0); `shouldRecordTimeToResolveOnlyWhenResolved` (MITIGATED event records nothing; RESOLVED after 90 min -> timer count 1, total 5400 s); `shouldReadActiveIncidentsFromTheRepository` (mock `countByState(OPEN)` = 3 -> gauge `opsdesk.incidents.active{state=OPEN}` value 3.0).
  - `IncidentServiceTest`: `shouldPublishIncidentOpened`, `shouldPublishStateChangeWithTimes` (mocked `ApplicationEventPublisher`, `ArgumentCaptor`).
  - `MetricsEndpointTest`: after `POST /api/incidents` (SEV2) and `PATCH` to RESOLVED, `GET /actuator/prometheus` contains `opsdesk_incidents_opened_total{application="opsdesk",severity="SEV2"}`, `opsdesk_incidents_time_to_resolve_seconds_count{application="opsdesk",severity="SEV2"}` and `opsdesk_incidents_active{application="opsdesk",state="OPEN"}`; `GET /actuator/env` is 404 (exposure minimal).
  - **Review Focus 1:** `shouldNotCountWhenTransactionRollsBack` in `MetricsEndpointTest`: open an incident inside a `TransactionTemplate` that is marked rollback-only -> counter unchanged.
- [ ] **Step 2:** `./mvnw -B -ntp -pl backend -am verify`. Expected: FAIL (compilation).
- [ ] **Step 3:** Implement. **Step 4:** same command, Expected: PASS.
- [ ] **Step 5: Commit** `feat(metrics): add incident business metrics and prometheus endpoint`.

### Task 2: ECS JSON logs with trace correlation

**Files:**
- Modify: `backend/pom.xml` (Micrometer Tracing with the OpenTelemetry bridge: the Boot 4 starter for OpenTelemetry tracing; confirm the artifact with `./mvnw dependency:tree` and the Boot 4.1 reference; no exporter dependency), `application.yml` (`management.tracing.sampling.probability: 1.0`, `management.tracing.export.enabled: false` or the Boot 4 equivalent so nothing is sent; `logging.structured.ecs.service.name: opsdesk`, `logging.structured.ecs.service.environment: ${OPSDESK_ENV:local}`)
- Modify: `Dockerfile` (`ENV LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`: the image always logs ECS JSON; `java -jar` locally stays human-readable)
- Modify: `backend/src/main/java/at/donmatzos/opsdesk/incident/IncidentService.java` (INFO log lines on open and state change with incident id, severity, from/to; no request bodies)
- Test: `backend/src/test/java/at/donmatzos/opsdesk/TracingTest.java` (extends `IntegrationTest`; `@Autowired ObservationRegistry`, `@Autowired Tracer`: `Observation.createNotStarted("test", registry).observe(...)` and inside it assert `tracer.currentSpan()` is not null and its `context().traceId()` matches `^[0-9a-f]{32}$`, i.e. the OpenTelemetry bridge is active, not the no-op tracer)

**Interfaces:**
- Produces: in the container image, every log line is one ECS JSON object with `service.name=opsdesk`, `log.level`, `message`, and for request-scoped lines `trace.id` and `span.id`.

- [ ] **Step 1: Write the failing test** `TracingTest.shouldCreateRealTraceIds`. **Step 2:** run, Expected: FAIL (no `Tracer` bean, or a no-op span).
- [ ] **Step 3:** Implement. **Step 4:** Expected: PASS; full `./mvnw verify` green.
- [ ] **Step 5:** `docker build -t opsdesk:dev .`, run it with the compose Postgres (`docker compose ... --profile app up -d --build`), `curl /api/incidents` once, `docker logs opsdesk-app-1 | tail -3 | python3 -m json.tool` shows ECS fields incl. `trace.id` on the request log line (Review Focus 3: two lines of one request share the trace id). **Commit** `feat(logging): ecs json logs with trace correlation`.

### Task 3: Local Elastic stack (compose profile `observability`)

**Files:**
- Modify: `deploy/compose/compose.yml` (services with `profiles: [observability]`: `elasticsearch` `elasticsearch:9.5.5` (`discovery.type=single-node`, `xpack.security.enabled=false`, `ES_JAVA_OPTS=-Xms512m -Xmx512m`, port `127.0.0.1:9200:9200`, healthcheck on `_cluster/health`), `kibana` `kibana:9.5.5` (`ELASTICSEARCH_HOSTS=http://elasticsearch:9200`, port `127.0.0.1:5601:5601`, healthcheck `api/status`), `filebeat` `elastic/filebeat:9.5.5` (`user: root`, read-only mounts `/var/lib/docker/containers` and `./filebeat.yml`, command `filebeat -e --strict.perms=false`); `app` also joins the profile)
- Create: `deploy/compose/filebeat.yml` (`filestream` input on `/var/lib/docker/containers/*/*.log` with the `container` parser; `decode_json_fields` on `message` into the root; `drop_event` unless `service.name == "opsdesk"`; output `elasticsearch` `http://elasticsearch:9200`, index `opsdesk-logs-%{+yyyy.MM.dd}`; `setup.ilm.enabled: false`, `setup.template.name/pattern` set)
- Create: `tools/elastic-verify.sh` (`compose --profile app --profile observability up -d --build --wait`; create an incident via the API; poll ES (`/opsdesk-logs-*/_search?q=service.name:opsdesk`) up to 90 s; assert at least one hit with a `trace.id` and that all hits of one `trace.id` share it; create the Kibana data view `opsdesk-logs-*` via `POST /api/data_views/data_view` (idempotent: skip when it exists); print the Kibana Discover URL; `down` only with `--down`)
- Modify: `docs/context/ops.md` (Observability section), `CLAUDE.md` (command)

- [ ] **Step 1: Failing check:** `tools/elastic-verify.sh` before the compose changes. Expected: fails (service `elasticsearch` unknown).
- [ ] **Step 2:** Implement compose + filebeat config.
- [ ] **Step 3:** `tools/elastic-verify.sh --down`. Expected: `ELASTIC VERIFY PASS`, hits with `trace.id`, data view created; `ss -ltn` during the run shows 9200/5601 bound to `127.0.0.1` only (Review Focus 5).
- [ ] **Step 4: Commit** `feat(observability): local elastic stack with filebeat and kibana`.

### Task 4: Datadog (mocked: registry profile and Unified Service Tagging)

**Files:**
- Modify: `backend/pom.xml` (`micrometer-registry-datadog`), `application.yml` (default document: `management.datadog.metrics.export.enabled: false`; new profile document `datadog`: `enabled: true`, `api-key: ${DATADOG_API_KEY}`, `uri: ${DATADOG_SITE_URI:https://api.datadoghq.eu}`, `step: 30s`; header comment `MOCK: no Datadog account ...`)
- Test: `backend/src/test/java/at/donmatzos/opsdesk/config/ApplicationConfigTest.java` (`shouldDisableDatadogByDefault`, `shouldTakeTheDatadogKeyOnlyFromTheEnvironment` (profile document `api-key` equals `${DATADOG_API_KEY}`)), `DatadogProfileTest.java` (Review Focus 4: `SpringApplicationBuilder` with profile `datadog`, without the env var -> context fails with a message containing `DATADOG_API_KEY`)
- Modify: Helm chart: values `datadog: { enabled: false, env: dev }`; when enabled: labels `tags.datadoghq.com/env|service|version` on Deployment and pod, env `DD_ENV`, `DD_SERVICE`, `DD_VERSION`, `DD_AGENT_HOST` (fieldRef `status.hostIP`), annotation `ad.datadoghq.com/opsdesk.logs: '[{"source":"java","service":"opsdesk"}]'`; schema; unit test `tests/datadog_test.yaml` (off by default: no `DD_*` env; on: labels + env + annotation); comment `MOCK`
- Modify: `docs/context/cloud-mocks.md` (Datadog row)

- [ ] **Step 1: Write the failing tests** (Java + helm-unittest). **Step 2:** `./mvnw -B -ntp verify` and `tools/helm-check.sh`. Expected: FAIL.
- [ ] **Step 3:** Implement. **Step 4:** Expected: PASS. **Step 5: Commit** `feat(datadog): mocked datadog registry and unified service tagging`.

### Task 5: Datadog monitors and dashboard as Terraform, validated in CI

**Files:**
- Create in `infra/terraform/datadog/`: `versions.tf` (`terraform >= 1.17`, provider `DataDog/datadog ~> 4.30`; `backend` block commented with `MOCK:` S3/remote example), `providers.tf` (`provider "datadog" {}` with comment: credentials from `DD_API_KEY`/`DD_APP_KEY`, `DD_HOST`), `variables.tf` (`env` (default `dev`), `service` (default `opsdesk`), `notify` (no default; a handle like `@ops-oncall`)), `monitors.tf` (`datadog_monitor` x3: active SEV1 incidents > 0 for 5 min; p95 `opsdesk.incidents.time.to.resolve` > 4 h; HTTP 5xx rate on `http.server.requests` > 5 % for 10 min; all tagged `service:opsdesk`, `env:${var.env}`, with runbook links into `docs/runbooks/`), `dashboard.tf` (`datadog_dashboard` with timeseries for opened incidents by severity, active incidents by state, MTTR p50/p95, request rate/latency), `outputs.tf`, `.tflint.hcl` (datadog ruleset off, terraform ruleset recommended), `README.md` (what it is, MOCK, how it would be applied)
- Create: `.terraform.lock.hcl` via `terraform init -backend=false` (committed)
- Modify: `.github/workflows/ci.yml` (job `terraform`: `hashicorp/setup-terraform` SHA-pinned `terraform_version: 1.17.1`, `terraform fmt -check -recursive infra/terraform`, `init -backend=false`, `validate`, tflint via its Docker image `ghcr.io/terraform-linters/tflint:v0.66.0`)
- Modify: `.github/dependabot.yml` (ecosystem `terraform` on `/infra/terraform/datadog`; `docker-compose` on `/deploy/compose`; npm `ignore` for `typescript` major versions with a comment that Angular dictates the TypeScript range)
- Modify: `docs/context/ops.md`, `docs/context/status.md`, `docs/context/skills-matrix.md` (Observability/Terraform rows -> real/validated)

- [ ] **Step 1:** Locally via Docker (`hashicorp/terraform:1.17.1`): `fmt -check`, `init -backend=false`, `validate`, tflint. Expected first: fails (no files). **Step 2:** implement. **Step 3:** Expected: all green; actionlint clean.
- [ ] **Step 4: Commit** `feat(terraform): datadog monitors and dashboard validated in ci`; push; CI green (publish/deploy as configured by the owner's variables).
- [ ] **Step 5:** status/docs, `python3 tools/context_check.py`, **commit** `docs: record phase 4 state`, push.
