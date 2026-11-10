# Testing (draft)
- Backend: **TestNG** + Mockito. `junit-jupiter` and `mockito-junit-jupiter` are excluded in `backend/pom.xml`, otherwise Surefire picks JUnit Platform and runs 0 tests.
- Kinds: plain unit (`IncidentTest`, Mockito mocks + fixed `Clock`: `IncidentServiceTest`), web slice (`@WebMvcTest` + `@MockitoBean` + `MockMvcTester`: `IncidentControllerTest`), full stack (extend `IntegrationTest`: random port, Testcontainers `postgres:18-alpine`, profile `demo`, `RestTestClient`). Integration tests assert on ids they created, not on counts. JaCoCo: phase 2.
- Naming: `<Class>Test`, methods `should<Behaviour>When<Condition>`.
- Frontend: Vitest via `ng test --watch=false` (jsdom), `HttpTestingController` for API calls; run by `./mvnw verify`.
- Ops checks in CI: `helm lint`, `terraform fmt -check && validate`, Trivy, `tools/smoke-test.sh` after the OpenShift deploy (when enabled).
- Context: `python3 tools/context_check.py`.
