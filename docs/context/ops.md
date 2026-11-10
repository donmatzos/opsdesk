# Ops (draft)
## CI/CD (real: GitHub Actions)
Now: `.github/workflows/build.yml` runs `./mvnw verify` on every push and PR (Temurin 25, Docker on the runner for Testcontainers, no secrets). Planned:
PR: build + tests, Trivy fs scan, Sonar (open question 6), `helm lint`, `terraform validate`, context check. Main: + image to GHCR, Trivy image scan, OpenShift deploy (`oc login` + `helm upgrade --install` + smoke test) only when `OPENSHIFT_SERVER`/`OPENSHIFT_TOKEN` secrets exist, else skipped. Tag: release (jar, image, Helm chart). Reusable workflow via `workflow_call`.
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
