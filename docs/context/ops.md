# Ops (draft)
## CI/CD (real: GitHub Actions)
`ci.yml` on every push and PR to `main`: `verify` (reusable `maven-verify.yml`: `./mvnw verify`, SonarCloud if `SONAR_TOKEN` exists, one JUnit check "Test report" for TestNG + Vitest), `context-check`, `secrets` (gitleaks, full history), `trivy-fs` (npm, secrets, Dockerfile misconfig; `pom.xml` skipped because of Maven Central rate limits), `image` (build, Trivy gate, `tools/ci-smoke.sh` with random UID + read-only FS + per-run DB password, push to `ghcr.io/donmatzos/opsdesk` only from `main` when repo variable `PUBLISH_IMAGE=true`, keep 10 versions). Trivy results go to GitHub code scanning (SARIF). Gates: HIGH/CRITICAL with a fix. Actions pinned by SHA; Dependabot (maven, npm, actions, docker) bumps them. Fork/Dependabot PRs: no secrets, Sonar skipped, report annotate-only, no SARIF upload.
Later: `helm lint`, `terraform validate`, OpenShift deploy (`oc login` + `helm upgrade --install` + smoke) when `OPENSHIFT_SERVER`/`OPENSHIFT_TOKEN` exist; tag releases (jar, image, chart).
## CI templating (mocked: GitLab, Jenkins)
`ci/gitlab/`: one shared template (stages build/test/scan/package/deploy) included by a consumer that sets only variables (thesis topic). `ci/jenkins/Jenkinsfile`: same stages.
## Container (real)
`Dockerfile`: build on `eclipse-temurin:25-jdk-noble` (`-DskipTests`, cache mounts), extract Boot layers (`jarmode=tools`), run on `eclipse-temurin:25-jre-ubi10-minimal` as `1001:0`, `/app` `g=u`, `HEALTHCHECK` via curl. Verified with `--user 123456:0 --read-only --tmpfs /tmp`. `.dockerignore` keeps `.env`, `.git`, `target`, `node_modules` out. `tools/smoke-test.sh <url>` checks health, API, UI. Compose profile `app` runs the image with Postgres; both read passwords from `deploy/compose/.env`.
## Kubernetes / OpenShift
Helm chart: Deployment (liveness/readiness/startup probes on actuator), Service, Ingress or OpenShift Route (switch in values), ConfigMap, Secret refs, NetworkPolicy, HPA (optional). Stages via values files. PostgreSQL: external, credentials from a Secret.
## Observability
Actuator + Micrometer business metrics (incidents opened, MTTR gauge); Prometheus endpoint; ECS JSON logs (Elastic); Datadog registry in profile `datadog` (disabled); Datadog monitors/dashboard in Terraform.
## Security
Trivy, Sonar, Dependabot, secrets only via CI/K8s secrets, image non-root, read-only root filesystem.
