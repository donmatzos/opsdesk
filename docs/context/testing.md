# Testing (draft)
- Backend: **TestNG** + Mockito; Spring tests extend `AbstractTestNGSpringContextTests`. Surefire runs TestNG. JaCoCo report.
- Naming: `<Class>Test`, methods `should<Behaviour>When<Condition>`.
- Frontend: Angular default unit test runner (decided in plan), run headless by frontend-maven-plugin in `mvn verify`.
- Ops checks in CI: `helm lint`, `terraform fmt -check && validate`, Trivy, `tools/smoke-test.sh` after the OpenShift deploy (when enabled).
- Context: `python3 tools/context_check.py`.
