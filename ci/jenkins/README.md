# Jenkins pipeline (mocked)

> MOCK: there is no Jenkins instance. `Jenkinsfile` is the pre-migration setup that `ci/gitlab/` replaces. It is parsed as Groovy
> in CI (`tools/ci-templates-check.sh jenkins`) but never run.

Same stages, tools and scripts as `.github/workflows/ci.yml`: build and test, SonarQube with a quality gate, gitleaks and Trivy,
Helm check, image build with a Trivy gate and the container smoke test, publish, deploy to dev with rollback, and a manual,
tag-only production deploy.

## Required plugins
Pipeline (declarative), Pipeline: Stage View, Credentials Binding, Config File Provider, SonarQube Scanner, JUnit, Coverage
(`recordCoverage`), Timestamper, Workspace Cleanup. The agent (label `linux && docker`) has Docker, `oc` and `helm`.

## Credential IDs (names only, values live in Jenkins)
| ID | Type | Used for |
|---|---|---|
| `sonarqube` | SonarQube server entry (Manage Jenkins) | `withSonarQubeEnv` |
| `artifactory-publisher` | username and password | `docker login` and Maven deploy to Artifactory (mocked) |
| `artifactory-maven-settings` | Config File Provider file | `settings.xml` with `${env.ARTIFACTORY_USER}` and `${env.ARTIFACTORY_TOKEN}` |
| `openshift-dev-server`, `openshift-dev-token` | secret text | dev deploy |
| `openshift-prod-server`, `openshift-prod-token` | secret text | prod deploy (manual, mocked) |

Job environment variables: `OPENSHIFT_DEPLOY` (`true` enables the dev deploy), `OPENSHIFT_NAMESPACE_DEV`, `OPENSHIFT_NAMESPACE_PROD`.
Every `sh` step uses single quotes, so Groovy never builds a command line with a secret in it.
