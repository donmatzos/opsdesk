# GitLab shared pipeline (mocked)

> MOCK: there is no GitLab instance. The files are complete and validated in CI (`tools/ci-templates-check.sh gitlab`: JSON
> schema of `vendor.gitlab-ci`, and the job set a real runner would build) but never run by GitLab.

The pipeline repeats `.github/workflows/ci.yml` stage by stage, with the same tools and the same scripts
(`tools/smoke-test.sh`, `tools/ci-smoke.sh`, `trivy.yaml`), so a project can move between GitHub Actions and GitLab without
changing what the pipeline checks.

| File | Role |
|---|---|
| `templates/maven-openshift.gitlab-ci.yml` | The shared template. In a real setup it lives in its own project (for example `devops/pipeline-templates`) and consumers pin it by tag. |
| `consumer/.gitlab-ci.yml` | What a project contains: an `include:` of the template and a `variables:` block. No job is defined here, except one documented override. |

## Jobs (same gates as the GitHub workflow)
`maven-verify` (tests, JUnit and JaCoCo reports) · `sonar` · `secret-scan` (gitleaks) · `trivy-fs` · `helm-lint` · `image-build`
(build, smoke test with a random UID and a read-only root filesystem) · `trivy-image` (fixable HIGH and CRITICAL fail) ·
`image-push` (default branch) · `maven-deploy` and `helm-publish` (Artifactory, mocked) · `deploy-dev` (default branch, rollback on
a failed smoke test) · `deploy-prod` (manual, tags only, mocked).

## Variables
Every variable has a default and a `description:` in the template; a consumer sets only what differs.

| Variable | Default | Meaning |
|---|---|---|
| `JAVA_VERSION` | `25` | JDK of the build image |
| `MAVEN_CLI_OPTS` | `-B -ntp` | options for every `./mvnw` call |
| `SONAR_ENABLED` | `false` | run SonarQube (needs the masked `SONAR_TOKEN`) |
| `TRIVY_SEVERITY` | `HIGH,CRITICAL` | severities that fail the pipeline when a fix exists |
| `HELM_CHART_DIR`, `HELM_RELEASE` | `deploy/helm/opsdesk`, `$CI_PROJECT_NAME` | chart and release name |
| `IMAGE_REPOSITORY` | `$CI_REGISTRY_IMAGE` | image repository |
| `SMOKE_TIMEOUT` | `180` | seconds the post-deploy smoke test waits |
| `OPENSHIFT_DEPLOY` | `false` | enable the deploy jobs |
| `OPENSHIFT_NAMESPACE_DEV`, `_PROD` | empty | namespaces |
| `ARTIFACTORY_ENABLED`, `ARTIFACTORY_URL`, `ARTIFACTORY_HOST` | `false`, example host | publish to Artifactory (mocked) |

Secrets are masked and protected CI/CD variables of the consumer project: `SONAR_TOKEN`, `OPENSHIFT_SERVER`, `OPENSHIFT_TOKEN`,
`ARTIFACTORY_USER`, `ARTIFACTORY_TOKEN`. Only their names appear in any file.

## Adopting it from a Jenkins project
Replace the `Jenkinsfile` with the consumer file, set the variables above, add the secrets as CI/CD variables. The stages are the
same as in `ci/jenkins/Jenkinsfile`, so the checks do not change. CI/CD components with `spec: inputs` would be the next step;
variables were chosen because they work on every GitLab version.

## Check locally
`tools/ci-templates-check.sh gitlab` (Docker only).
