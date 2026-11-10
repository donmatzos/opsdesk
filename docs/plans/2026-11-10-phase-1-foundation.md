# Phase 1: Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A Maven multi-module build that produces one runnable `opsdesk.jar`: Spring Boot backend (service catalog + incident lifecycle on PostgreSQL) with an Angular shell inside, tested with TestNG.

**Architecture:** Parent POM with modules `frontend` (Angular built by frontend-maven-plugin into `META-INF/resources` of a plain jar) and `backend` (Spring Boot app that depends on `frontend` and is repackaged as `opsdesk.jar`). Packages by feature: `catalog`, `incident`, `common` (errors), `config`. Spring Data JDBC on PostgreSQL, schema via Flyway, integration tests via Testcontainers.

**Tech Stack:** Java 25, Spring Boot 4.1.2, Spring Data JDBC, Flyway, PostgreSQL 18, TestNG 7.12.0, Mockito, Testcontainers, Angular 22.2 (Vitest), Node 24.22.2, frontend-maven-plugin 2.0.2, Maven 3.9.16 (wrapper).

**Spec:** `docs/context/architecture.md`, `docs/adr/0001-initial-decisions.md`, `docs/plans/2026-11-10-roadmap.md` (row 1).

## Global Constraints
- groupId `at.donmatzos.opsdesk`, root package `at.donmatzos.opsdesk`, version `0.1.0-SNAPSHOT`.
- Backend tests use TestNG only: exclude `org.junit.jupiter:junit-jupiter` from `spring-boot-starter-test`; Surefire must log TestNG as provider.
- Integration tests extend `IntegrationTest` (Task 1); Docker must be running for `./mvnw verify`.
- No secrets in files, not even dev ones: DB credentials come from env (`SPRING_DATASOURCE_*`); compose reads `POSTGRES_PASSWORD` from the git-ignored `deploy/compose/.env`. (Corrected 2026-11-10: the first version of this plan prescribed literal dev defaults, which got committed and had to be scrubbed from the branch history.)
- Never commit to `main`; branch `feature/foundation`; stage explicit paths.
- Errors are RFC 9457 `ProblemDetail` JSON (`spring.mvc.problemdetails.enabled: true`).

## Review Focus
1. Unknown enum in JSON (`"severity":"SEV9"`) or query (`?state=foo`) must be 400 ProblemDetail, not 500 (Task 4).
2. Unknown `serviceId` on incident create must be 422, not 500 from the FK violation (Task 4).
3. Same-state or backward transitions (`OPEN->OPEN`, `RESOLVED->OPEN`) must be 409 (Tasks 3, 4).
4. Browser refresh on a deep link (`/incidents/7`) must return `index.html`, while `/api/unknown` stays a 404 ProblemDetail (Task 5).
5. Blank summary or summary over 200 chars must be 400 naming the field (Task 4).

## Prerequisite (owner)
Create GitHub repo `donmatzos/opsdesk`, commit the current skeleton as the initial `main` commit, push. The agent then works on `feature/foundation` from it.

---

### Task 1: Build skeleton with TestNG and Testcontainers

**Files:**
- Create: `.mvn/wrapper/maven-wrapper.properties`, `mvnw`, `mvnw.cmd` (via `mvn wrapper:wrapper -Dmaven=3.9.16`), `pom.xml`, `backend/pom.xml`
- Create: `backend/src/main/java/at/donmatzos/opsdesk/OpsdeskApplication.java`, `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/at/donmatzos/opsdesk/TestcontainersConfig.java`, `.../IntegrationTest.java`, `.../ApplicationContextTest.java`
- Create: `deploy/compose/compose.yml`
- Delete: `.gitkeep` files in directories that now have content

**Interfaces:**
- Produces: `abstract class IntegrationTest extends AbstractTestNGSpringContextTests` annotated `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@Import(TestcontainersConfig.class)`, `@ActiveProfiles("demo")`; method `protected RestTestClient client()` bound to `http://localhost:<port>`.
- Produces: `TestcontainersConfig` bean `@ServiceConnection PostgreSQLContainer` with image `postgres:18-alpine`.

- [ ] **Step 1:** `git checkout -b feature/foundation`; generate the wrapper; write parent `pom.xml` (parent `spring-boot-starter-parent` 4.1.2, `java.version` 25, modules `backend` only for now, properties for `testng.version` 7.12.0, `frontend-maven-plugin.version` 2.0.2, `node.version` v24.22.2).
- [ ] **Step 2:** `backend/pom.xml`: `spring-boot-starter-webmvc`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `postgresql` (runtime); test: `spring-boot-starter-test` (junit-jupiter excluded), `testng`, `spring-boot-testcontainers`, `org.testcontainers:testcontainers-postgresql`. `spring-boot-maven-plugin` with `<finalName>opsdesk</finalName>` on the build.
- [ ] **Step 3: Write the failing test** `ApplicationContextTest extends IntegrationTest`:
```java
@Test
public void shouldReportHealthUp() {
    client().get().uri("/actuator/health").exchange()
        .expectStatus().isOk()
        .expectBody().jsonPath("$.status").isEqualTo("UP");
}
```
- [ ] **Step 4:** Run `./mvnw -pl backend verify`. Expected: FAIL (no application class).
- [ ] **Step 5:** Add `OpsdeskApplication` (`@SpringBootApplication`) and `application.yml` (`spring.application.name: opsdesk`, problemdetails enabled, datasource from env, no fallbacks; profile `demo` defaults only URL and user).
- [ ] **Step 6:** Run `./mvnw -pl backend verify`. Expected: PASS, and the Surefire output shows `TestNG` (if it shows `JUnit Platform` and 0 tests, the exclusion is missing).
- [ ] **Step 7:** `deploy/compose/compose.yml`: service `postgres` (`postgres:18-alpine`, db/user `opsdesk`, password only via `env_file: .env` (required), port 5432, named volume, `pg_isready` healthcheck). Verify: `docker compose -f deploy/compose/compose.yml up -d` then `docker compose ... ps` shows `healthy`.
- [ ] **Step 8: Commit** `build: add maven multi-module skeleton with testng and testcontainers`.

### Task 2: Service catalog

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__catalog.sql`, `backend/src/main/resources/db/demo/R__demo_data.sql`
- Create in `backend/src/main/java/at/donmatzos/opsdesk/catalog/`: `Stage.java`, `ServiceStatus.java`, `MonitoredService.java`, `MonitoredServiceRepository.java`, `CatalogController.java`
- Create in `.../common/`: `NotFoundException.java`, `ApiExceptionHandler.java`
- Modify: `backend/pom.xml` (add `spring-boot-starter-data-jdbc`, `spring-boot-starter-flyway`, `flyway-database-postgresql`), `application.yml` (profile `demo`: `spring.flyway.locations: classpath:db/migration,classpath:db/demo`)
- Test: `backend/src/test/java/at/donmatzos/opsdesk/catalog/CatalogApiTest.java`

**Interfaces:**
- Produces: `enum Stage { DEV, PROD }`, `enum ServiceStatus { UP, DEGRADED, DOWN }`.
- Produces: `@Table("services") record MonitoredService(@Id Long id, String name, String ownerTeam, Stage stage, ServiceStatus status)`.
- Produces: `MonitoredServiceRepository extends ListCrudRepository<MonitoredService, Long>` with `List<MonitoredService> findAllByOrderByNameAsc()`.
- Produces: `class NotFoundException extends RuntimeException` with `NotFoundException(String resource, Object id)` -> message `"<resource> <id> not found"`; `ApiExceptionHandler extends ResponseEntityExceptionHandler` (`@RestControllerAdvice`) maps it to 404 ProblemDetail.
- API: `GET /api/services` (sorted by name), `GET /api/services/{id}`.

- [ ] **Step 1: Write the failing tests** in `CatalogApiTest extends IntegrationTest`:
  - `shouldListDemoServicesSortedByName`: 200, `$.length()` == 4, `$[0].name` == `"billing-api"` (alphabetically first of the demo set).
  - `shouldReturnProblemDetailForUnknownService`: `GET /api/services/999999` -> 404, `$.title` == `"Not Found"`, `$.detail` == `"Service 999999 not found"`.
- [ ] **Step 2:** Run `./mvnw -pl backend verify -Dtest=CatalogApiTest`. Expected: FAIL (404 on `/api/services`).
- [ ] **Step 3:** `V1__catalog.sql`: table `services` (`id bigint generated always as identity primary key`, `name varchar(100) not null unique`, `owner_team varchar(100) not null`, `stage varchar(10) not null`, `status varchar(10) not null`, check constraints for the enum values). `R__demo_data.sql`: idempotent insert (`on conflict (name) do nothing`) of `billing-api` (team-payments, PROD, UP), `customer-portal` (team-web, PROD, DEGRADED), `order-service` (team-orders, PROD, UP), `report-batch` (team-data, DEV, DOWN).
- [ ] **Step 4:** Implement enums, record, repository, `CatalogController`, `NotFoundException`, `ApiExceptionHandler`.
- [ ] **Step 5:** Run `./mvnw -pl backend verify`. Expected: PASS.
- [ ] **Step 6: Commit** `feat(catalog): add service catalog api on postgres`.

### Task 3: Incident lifecycle rules

**Files:**
- Create in `backend/src/main/java/at/donmatzos/opsdesk/incident/`: `Severity.java`, `IncidentState.java`, `Incident.java`, `IllegalStateTransitionException.java`
- Create: `backend/src/main/resources/db/migration/V2__incidents.sql`
- Test: `backend/src/test/java/at/donmatzos/opsdesk/incident/IncidentStateTest.java`, `IncidentTest.java` (plain TestNG, no Spring)

**Interfaces:**
- Produces: `enum Severity { SEV1, SEV2, SEV3 }`.
- Produces: `enum IncidentState { OPEN, MITIGATED, RESOLVED; boolean canTransitionTo(IncidentState next) }`. Allowed: `OPEN->MITIGATED`, `OPEN->RESOLVED`, `MITIGATED->RESOLVED`. Everything else false.
- Produces: `@Table("incidents") record Incident(@Id Long id, Long serviceId, Severity severity, IncidentState state, String summary, Instant openedAt, Instant resolvedAt)` with `static Incident open(long serviceId, Severity severity, String summary, Instant now)` (state OPEN, resolvedAt null) and `Incident transitionTo(IncidentState next, Instant now)` (sets `resolvedAt = now` when next is RESOLVED; throws `IllegalStateTransitionException(IncidentState from, IncidentState to)` with message `"Cannot change incident state from <from> to <to>"`).

- [ ] **Step 1: Write the failing tests.** `IncidentStateTest` uses one `@DataProvider` with all 9 `(from, to, expected)` pairs and asserts `from.canTransitionTo(to) == expected`. `IncidentTest`: `shouldSetResolvedAtWhenResolved` (resolvedAt == the passed `now`), `shouldKeepResolvedAtNullWhenMitigated`, `shouldRejectReopeningResolvedIncident` (`expectedExceptions = IllegalStateTransitionException.class`, message as above).
- [ ] **Step 2:** Run `./mvnw -pl backend test -Dtest='Incident*Test'`. Expected: FAIL (compilation).
- [ ] **Step 3:** Implement the enums, record and exception. `V2__incidents.sql`: table `incidents` (`id` identity pk, `service_id bigint not null references services(id)`, `severity varchar(4)`, `state varchar(10)`, `summary varchar(200) not null`, `opened_at timestamptz not null`, `resolved_at timestamptz`, checks for enums, index on `state`).
- [ ] **Step 4:** Run the same command. Expected: PASS (11 tests).
- [ ] **Step 5: Commit** `feat(incident): add incident lifecycle rules and schema`.

### Task 4: Incident API

**Files:**
- Create in `.../incident/`: `IncidentRepository.java`, `IncidentService.java`, `IncidentController.java`, `CreateIncidentRequest.java`, `ChangeStateRequest.java`, `UnknownServiceException.java`
- Create: `.../config/ClockConfig.java` (`@Bean Clock clock()` -> `Clock.systemUTC()`)
- Modify: `.../common/ApiExceptionHandler.java` (409 for `IllegalStateTransitionException`, 422 for `UnknownServiceException`, 400 for `MethodArgumentTypeMismatchException`)
- Test: `backend/src/test/java/at/donmatzos/opsdesk/incident/IncidentControllerTest.java` (`@WebMvcTest(IncidentController.class)` extending `AbstractTestNGSpringContextTests`, `@MockitoBean IncidentService`, `MockMvcTester`), `IncidentApiTest.java` (extends `IntegrationTest`)

**Interfaces:**
- Consumes: `Incident`, `IncidentState`, `Severity` (Task 3); `MonitoredServiceRepository`, `NotFoundException` (Task 2).
- Produces: `record CreateIncidentRequest(@NotNull Long serviceId, @NotNull Severity severity, @NotBlank @Size(max = 200) String summary)`, `record ChangeStateRequest(@NotNull IncidentState state)`.
- Produces: `IncidentRepository extends ListCrudRepository<Incident, Long>` with `findAllByOrderByOpenedAtDesc()` and `findByStateOrderByOpenedAtDesc(IncidentState state)`.
- Produces: `IncidentService` with `Incident open(CreateIncidentRequest request)` (throws `UnknownServiceException(long serviceId)`, message `"Service <id> does not exist"`), `List<Incident> list(@Nullable IncidentState state)`, `Incident changeState(long id, IncidentState next)` (throws `NotFoundException("Incident", id)`).
- API: `POST /api/incidents` -> 201 + `Location: /api/incidents/{id}`; `GET /api/incidents[?state=]`; `GET /api/incidents/{id}`; `PATCH /api/incidents/{id}` body `ChangeStateRequest` -> 200.

- [ ] **Step 1: Write the failing slice tests** (`IncidentControllerTest`, service mocked): `shouldReturn400ForUnknownSeverity` (`"severity":"SEV9"`), `shouldReturn400ForBlankSummary` and `shouldReturn400ForSummaryOver200Chars` (both assert the response names field `summary`), `shouldReturn400ForUnknownStateFilter` (`?state=foo`), `shouldReturn409WhenServiceRejectsTransition` (mock throws `IllegalStateTransitionException(RESOLVED, OPEN)`; `$.detail` == `"Cannot change incident state from RESOLVED to OPEN"`), `shouldReturn422ForUnknownService`. All bodies are ProblemDetail (`Content-Type: application/problem+json`).
- [ ] **Step 2: Write the failing integration tests** (`IncidentApiTest`): `shouldOpenMitigateAndResolveIncident` (create for demo `billing-api` -> 201 + Location; PATCH MITIGATED -> 200; PATCH RESOLVED -> 200 with non-null `resolvedAt`; PATCH OPEN -> 409), `shouldFilterByState` (created incident appears in `?state=OPEN` and not in `?state=RESOLVED`; assert by id, not by count), `shouldReturn422ForUnknownServiceId` (`serviceId` 999999).
- [ ] **Step 3:** Run `./mvnw -pl backend verify`. Expected: FAIL (no controller).
- [ ] **Step 4:** Implement. `IncidentService` checks `MonitoredServiceRepository.existsById` before insert (do not rely on the FK error), uses the injected `Clock`.
- [ ] **Step 5:** Run `./mvnw -pl backend verify`. Expected: PASS.
- [ ] **Step 6: Commit** `feat(incident): add incident rest api with problem details`.

### Task 5: Angular shell inside the jar

**Files:**
- Create: `frontend/` Angular workspace (`npx @angular/cli@22.2 new opsdesk-ui --directory frontend --routing --style=scss --ssr=false --skip-git --skip-install`; remove `frontend/src/app/.gitkeep` first), `frontend/pom.xml`
- Create: `frontend/src/app/catalog/catalog-api.ts`, `frontend/src/app/catalog/services-page.ts`, `frontend/src/app/catalog/services-page.spec.ts`, `frontend/proxy.conf.json` (`/api` -> `http://localhost:8080`)
- Modify: `frontend/angular.json` (`outputPath: { "base": "target/classes/META-INF/resources", "browser": "" }`), `frontend/src/app/app.routes.ts`, root `pom.xml` (modules `frontend`, `backend` in that order), `backend/pom.xml` (depend on `frontend`)
- Create: `backend/src/main/java/at/donmatzos/opsdesk/config/SpaForwardController.java`
- Test: `backend/src/test/java/at/donmatzos/opsdesk/config/SpaRoutingTest.java` (extends `IntegrationTest`)
- Modify: `docs/context/status.md`, `docs/context/project-structure.md`, `docs/context/frontend.md`, `CLAUDE.md` (Commands section: mark working)

**Interfaces:**
- Produces: `CatalogApi` (`@Injectable({providedIn: 'root'})`) with `services(): Observable<MonitoredService[]>` calling `GET /api/services`; TS interface `MonitoredService { id: number; name: string; ownerTeam: string; stage: 'DEV' | 'PROD'; status: 'UP' | 'DEGRADED' | 'DOWN' }`.
- Produces: Angular routes `''` -> redirect `services`, `services` -> `ServicesPage`, `incidents` and `incidents/:id` -> placeholder component (filled in phase 7).
- Produces: `SpaForwardController` forwarding `GET /`, `/services`, `/incidents`, `/incidents/{id}` to `/index.html`. New Angular routes must be added here (note in `frontend.md`).

- [ ] **Step 1: Write the failing frontend test** `services-page.spec.ts` (Vitest, `HttpTestingController`): renders one row per service returned by `/api/services`, row shows name and status.
- [ ] **Step 2: Write the failing backend test** `SpaRoutingTest`: `GET /incidents/7` -> 200, `text/html`, body contains `<app-root`; `GET /api/unknown` -> 404 `application/problem+json`.
- [ ] **Step 3:** `frontend/pom.xml` (packaging `jar`): frontend-maven-plugin executions `install-node-and-npm` (`${node.version}`), `npm ci`, `npm run build` (phase `generate-resources`), `npm test -- --watch=false` (phase `test`). Implement `CatalogApi`, `ServicesPage` (standalone, signals, table), routes, `SpaForwardController`.
- [ ] **Step 4:** Run `./mvnw verify`. Expected: PASS for both modules; `unzip -l backend/target/opsdesk.jar | grep 'BOOT-INF/lib/frontend-'` prints one line.
- [ ] **Step 5: Manual check:** `docker compose -f deploy/compose/compose.yml up -d`, `SPRING_PROFILES_ACTIVE=demo java -jar backend/target/opsdesk.jar`; `curl -s localhost:8080/api/services` lists 4 services; `http://localhost:8080/services` shows the table; refresh keeps the page.
- [ ] **Step 6:** Update context files (status: phase 1 done, next phase 2; structure: `common` package, `docs/plans/`; frontend: SPA route list rule; `python3 tools/context_check.py` passes).
- [ ] **Step 7: Commit** `feat(frontend): bundle angular shell into the application jar`, then push `feature/foundation`.
