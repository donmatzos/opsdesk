# Testing (draft)
- Backend: **TestNG** + Mockito. `junit-jupiter` and `mockito-junit-jupiter` are excluded in `backend/pom.xml`, otherwise Surefire picks JUnit Platform and runs 0 tests.
- Kinds: plain unit (`IncidentTest`, Mockito mocks + fixed `Clock`: `IncidentServiceTest`), web slice (`@WebMvcTest` + `@MockitoBean` + `MockMvcTester`: `IncidentControllerTest`), full stack (extend `IntegrationTest`: random port, Testcontainers `postgres:18-alpine`, profile `demo`, `RestTestClient`). Integration tests assert on ids they created, not on counts.
- Naming: `<Class>Test`, methods `should<Behaviour>[When<Condition>]` (condition part only when there is one).
- Frontend: Vitest via `ng test --watch=false` (jsdom), `HttpTestingController` for API calls; run by `./mvnw verify`.
- Ops checks in CI: `helm lint`, `terraform fmt -check && validate`, Trivy, `tools/smoke-test.sh` after the OpenShift deploy (when enabled).
- Context: `python3 tools/context_check.py`.
- Reports after `./mvnw verify`: backend JUnit XML `backend/target/surefire-reports/TEST-*.xml`, coverage `backend/target/site/jacoco/jacoco.xml`; frontend JUnit XML `frontend/target/test-reports/vitest-junit.xml`, coverage `frontend/coverage/opsdesk-ui/lcov.info`. CI publishes both JUnit reports as one check; SonarCloud reads both coverage files.
- `-DskipTests` skips backend and frontend tests (used by the Docker build).
- Helm chart: `tools/helm-check.sh` (Docker only: `helm lint` per values file, helm-unittest in `deploy/helm/opsdesk/tests/`, kubeconform incl. the OpenShift Route schema). Local cluster check: `docker build -t opsdesk:dev . && tools/kind-verify.sh` (kind, restricted Pod Security, install + upgrade + smoke, DB password kept; `KEEP_CLUSTER=1` keeps a failed cluster). kind is local only; CI deploys to OpenShift.
