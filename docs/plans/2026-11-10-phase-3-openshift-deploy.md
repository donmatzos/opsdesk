# Phase 3: OpenShift Deploy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A tested Helm chart that runs opsdesk on OpenShift (and plain Kubernetes), plus a CI deploy job (`oc` + `helm upgrade --install --atomic` + smoke test + rollback) that runs against a real cluster when the owner switches it on, and is skipped cleanly otherwise.

**Architecture:** One chart `deploy/helm/opsdesk` with secure defaults in `values.yaml` and stage files `values-dev.yaml` (dev database in the namespace, Route) and `values-prod.yaml` (external database Secret, HPA, PDB; never deployed). Templates are developed test-first with helm-unittest; rendered output is checked by `helm lint`, kubeconform (incl. the OpenShift Route schema) and Trivy misconfig. CI renders and validates on every run; the `deploy-dev` job targets an OpenShift namespace (e.g. Red Hat Developer Sandbox) via repository variable + secrets.

**Tech Stack:** Helm 4.3.1 (`alpine/helm:4.3.1` locally, `azure/setup-helm` v5.1.0 in CI), helm-unittest 1.2.1 (`helmunittest/helm-unittest:4.3.0-1.2.1`), kubeconform 0.8.0 (`ghcr.io/yannh/kubeconform`), OpenShift client `oc` 5.0.1 from `mirror.openshift.com` (sha256 verified), `quay.io/sclorg/postgresql-18-c10s:20261104` (OpenShift-compatible PostgreSQL for the dev stage), kind 0.33.0 (local verification only, never in CI).

**Spec:** `docs/plans/2026-11-10-roadmap.md` (row 3), `docs/adr/0001-initial-decisions.md` (CI deploy = OpenShift via `oc` + Helm, gated), `docs/context/ops.md` (Kubernetes / OpenShift), `docs/context/cloud-mocks.md` (OpenShift row).

## Global Constraints
- No secrets in the repo, ever: no password in values files, tests or docs. The dev DB password is generated in-cluster (Helm `lookup` keeps an existing Secret, else `randAlphaNum 32`); prod uses an existing Secret. No `password: <literal>` anywhere, also not in helm-unittest fixtures (use `set` with a non-secret-looking placeholder only where unavoidable, e.g. `database.existingSecret: opsdesk-db`).
- Pods are OpenShift restricted-v2 compatible: no `runAsUser`/`fsGroup` set by default, `runAsNonRoot: true`, `allowPrivilegeEscalation: false`, `capabilities.drop: [ALL]`, `seccompProfile: RuntimeDefault`, `readOnlyRootFilesystem: true` with an `emptyDir` on `/tmp`; `automountServiceAccountToken: false`.
- Every container has resource requests and a memory limit; probes on `/actuator/health/liveness`, `/actuator/health/readiness`, startup probe on liveness.
- Trivy misconfig (`HIGH,CRITICAL`) on the chart must stay clean (the existing `trivy-fs` gate scans it).
- CI deploy runs only on `push` to `main` with `vars.OPENSHIFT_DEPLOY == 'true'`, after `publish` (it needs the GHCR image). Secrets: `OPENSHIFT_SERVER`, `OPENSHIFT_TOKEN`; variable `OPENSHIFT_NAMESPACE`.
- Actions pinned by SHA (resolve with `git ls-remote`, the REST API is rate-limited on this machine); downloaded binaries verified by checksum.
- Branch `feature/openshift-deploy`; one commit per task.

## Review Focus
1. Rendering without a database Secret (`devDatabase.enabled=false`, `database.existingSecret` empty) must fail with a clear message, not deploy a pod that crash-loops (Task 2).
2. A second `helm upgrade` must keep the generated dev DB password (no new random value that locks the app out of an existing volume) (Task 3).
3. The pod must start under OpenShift's random UID with a read-only root FS; nothing in the chart may pin a UID (Tasks 2, 4).
4. NetworkPolicy must still let the OpenShift router reach the app and the app reach the dev DB; everything else into the DB is denied (Task 2, 3, 4).
5. A failed rollout or failed smoke test must leave the previous release running (`--atomic`, then `helm rollback` on smoke failure) (Task 5).

## Prerequisites (owner, only for Task 5's real deploy; nothing blocks Tasks 1-4)
- Red Hat Developer Sandbox (free, renew every 30 days) or any OpenShift namespace. Setup steps are written in Task 5 to `deploy/openshift/README.md`.
- `PUBLISH_IMAGE=true` and a public GHCR package (the cluster pulls the image without credentials).

---

### Task 1: Kubernetes probes in the application

**Files:**
- Modify: `backend/src/main/resources/application.yml` (`management.endpoint.health.probes.enabled: true`, `management.health.livenessstate.enabled: true`, `management.health.readinessstate.enabled: true`, `server.shutdown: graceful`, `spring.lifecycle.timeout-per-shutdown-phase: 20s`)
- Test: `backend/src/test/java/at/donmatzos/opsdesk/ApplicationContextTest.java`

**Interfaces:**
- Produces: `GET /actuator/health/liveness` and `GET /actuator/health/readiness` -> 200 `{"status":"UP"}` (also outside Kubernetes, so probes are testable locally and in the smoke test).

- [ ] **Step 1: Write the failing tests** `shouldExposeLivenessProbe` and `shouldExposeReadinessProbe` (`/actuator/health/liveness|readiness` -> 200, `$.status` == `"UP"`).
- [ ] **Step 2:** `./mvnw -B -ntp -pl backend -am verify -Dtest='ApplicationContextTest' -Dsurefire.failIfNoSpecifiedTests=false`. Expected: FAIL (404).
- [ ] **Step 3:** Add the config. **Step 4:** same command, Expected: PASS; full `./mvnw -B -ntp verify` green.
- [ ] **Step 5:** `tools/smoke-test.sh`: also check `/actuator/health/readiness` is UP. **Commit** `feat(ops): expose kubernetes probes and graceful shutdown`.

### Task 2: Helm chart core (test-first)

**Files:**
- Create in `deploy/helm/opsdesk/`: `Chart.yaml` (`apiVersion: v2`, `name: opsdesk`, `version: 0.1.0`, `appVersion: "0.1.0"`, `kubeVersion: ">=1.29.0-0"`), `values.yaml`, `values.schema.json`, `.helmignore`, `templates/_helpers.tpl`, `templates/deployment.yaml`, `templates/service.yaml`, `templates/serviceaccount.yaml`, `templates/route.yaml`, `templates/ingress.yaml`, `templates/networkpolicy.yaml`, `templates/NOTES.txt`
- Create: `deploy/helm/opsdesk/tests/deployment_test.yaml`, `tests/exposure_test.yaml`, `tests/networkpolicy_test.yaml`
- Create: `tools/helm-check.sh` (Bash, Docker-based; runs `helm lint` for default/dev/prod values, `helm unittest`, `helm template | kubeconform -strict -summary` with the default schemas plus `https://raw.githubusercontent.com/datreeio/CRDs-catalog/main/{{.Group}}/{{.ResourceKind}}_{{.ResourceAPIVersion}}.json` for `Route`; exits non-zero on the first failure)
- Remove: `.gitkeep` in `deploy/helm/opsdesk/templates`

**Interfaces (values keys, used by Tasks 3-5):**
```yaml
image: { repository: ghcr.io/donmatzos/opsdesk, tag: "", pullPolicy: IfNotPresent }   # tag "" -> .Chart.AppVersion
replicaCount: 1
springProfiles: ""                 # -> SPRING_PROFILES_ACTIVE when set
database: { url: "", username: opsdesk, existingSecret: "", passwordKey: password }
devDatabase: { enabled: false }    # filled in Task 3
route: { enabled: true, host: "" } # OpenShift Route, edge TLS, redirect http
ingress: { enabled: false, className: "", host: "" }
networkPolicy: { enabled: true }
resources: { requests: { cpu: 100m, memory: 384Mi }, limits: { memory: 768Mi } }
podAnnotations: {}
```
- Helpers: `opsdesk.fullname`, `opsdesk.labels` (app.kubernetes.io/{name,instance,version,managed-by}, `helm.sh/chart`), `opsdesk.selectorLabels`, `opsdesk.dbSecretName` (devDatabase -> `<fullname>-db`; else `database.existingSecret`; else `fail "database.existingSecret is required when devDatabase.enabled is false"`), `opsdesk.dbUrl` (devDatabase -> `jdbc:postgresql://<fullname>-db:5432/opsdesk`; else `database.url`, required).
- Env: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (`secretKeyRef` to `opsdesk.dbSecretName` / `database.passwordKey`), `SPRING_PROFILES_ACTIVE` when set.

- [ ] **Step 1: Write the failing helm-unittest suites** (assertions, exact values):
  - `deployment_test.yaml`: image is `ghcr.io/donmatzos/opsdesk:0.1.0` by default and `...:sha-abc1234` with `image.tag=sha-abc1234`; container `securityContext` equals the Global Constraints set; pod `securityContext` has no `runAsUser`; `automountServiceAccountToken: false`; `/tmp` is an `emptyDir` mount; probes paths `/actuator/health/liveness` and `/actuator/health/readiness` on port `http` (8080); `SPRING_DATASOURCE_PASSWORD` comes from `secretKeyRef` name `opsdesk-db`, key `password` (with `database.existingSecret=opsdesk-db`, `database.url=jdbc:postgresql://db:5432/opsdesk`); **failedTemplate** with message `database.existingSecret is required when devDatabase.enabled is false` when both are unset (Review Focus 1).
  - `exposure_test.yaml`: Route rendered by default (kind `Route`, `tls.termination: edge`, `insecureEdgeTerminationPolicy: Redirect`, `to.name` = service); with `route.enabled=false, ingress.enabled=true, ingress.host=opsdesk.example.test` an Ingress and no Route.
  - `networkpolicy_test.yaml`: app policy allows ingress on 8080 from namespaceSelector `policy-group.network.openshift.io/ingress: ""` and from pods in the same namespace; disabled with `networkPolicy.enabled=false`.
- [ ] **Step 2:** `tools/helm-check.sh`. Expected: FAIL (no templates).
- [ ] **Step 3:** Implement templates, values, schema (`values.schema.json` rejects unknown top-level keys and wrong types), helpers, NOTES.
- [ ] **Step 4:** `tools/helm-check.sh`. Expected: lint OK for all values files, all unit tests PASS, kubeconform `Invalid: 0, Errors: 0`.
- [ ] **Step 5:** Trivy misconfig on the chart: `docker run ... aquasec/trivy config --severity HIGH,CRITICAL --exit-code 1 /repo/deploy/helm/opsdesk`. Expected: exit 0. **Commit** `feat(helm): add openshift-ready chart with unit tests`.

### Task 3: Dev database and stage values

**Files:**
- Create: `templates/dev-database.yaml` (StatefulSet `<fullname>-db`, 1 replica, image `quay.io/sclorg/postgresql-18-c10s:20261104`, env `POSTGRESQL_USER`, `POSTGRESQL_DATABASE`, `POSTGRESQL_PASSWORD` from the Secret, PVC `devDatabase.storage` (default `1Gi`), readiness `pg_isready -h 127.0.0.1`, same restricted securityContext except `readOnlyRootFilesystem` (sclorg needs writable `/var/run/postgresql` -> emptyDir; keep read-only if that works, ledger the outcome); Service `<fullname>-db` on 5432; NetworkPolicy: DB ingress only from pods with the app selector labels)
- Create: `templates/dev-database-secret.yaml` (only with `devDatabase.enabled`; `lookup "v1" "Secret" .Release.Namespace <name>`; reuse existing `data.password`, else `randAlphaNum 32 | b64enc`; annotation `helm.sh/resource-policy: keep`)
- Create: `templates/hpa.yaml` (`autoscaling.enabled`, `minReplicas`, `maxReplicas`, CPU `targetCPUUtilizationPercentage`), `templates/pdb.yaml` (`podDisruptionBudget.enabled`, `minAvailable`)
- Create: `deploy/helm/opsdesk/values-dev.yaml` (`springProfiles: demo`, `devDatabase.enabled: true`, `route.enabled: true`), `values-prod.yaml` (`replicaCount: 2`, `database.url: jdbc:postgresql://postgres.prod.internal:5432/opsdesk`, `database.existingSecret: opsdesk-db`, autoscaling 2-4 at 75 %, PDB `minAvailable: 1`, larger resources; header comment: MOCK — no prod cluster, shows the shape)
- Create: `tests/dev_database_test.yaml`, `tests/stages_test.yaml`
- Modify: `values.yaml`, `values.schema.json` (`devDatabase`, `autoscaling`, `podDisruptionBudget`)

**Interfaces:**
- Consumes: `opsdesk.dbSecretName`, `opsdesk.dbUrl`, `opsdesk.selectorLabels` (Task 2).
- Produces: values files used by Task 5 (`-f values-dev.yaml`).

- [ ] **Step 1: Write the failing suites:** dev database renders StatefulSet + Service + Secret + DB NetworkPolicy only with `devDatabase.enabled=true`; app env URL `jdbc:postgresql://opsdesk-db:5432/opsdesk`; Secret has annotation `helm.sh/resource-policy: keep` and a 32-char password when no existing Secret; **with a `kubernetesProvider` mock object of an existing Secret, the rendered password equals the existing one** (Review Focus 2); `values-prod.yaml` renders HPA (min 2, max 4) and PDB and no dev DB; `values-dev.yaml` renders no HPA.
- [ ] **Step 2:** `tools/helm-check.sh`. Expected: FAIL. **Step 3:** implement. **Step 4:** Expected: PASS, kubeconform clean for dev and prod, Trivy config clean.
- [ ] **Step 5: Commit** `feat(helm): add dev database, stage values, hpa and pdb`.

### Task 4: Local cluster verification (kind; not part of CI)

**Files:**
- Create: `tools/kind-verify.sh` (downloads kind 0.33.0 to `${XDG_CACHE_HOME:-~/.cache}/opsdesk/` with checksum; creates cluster `opsdesk-verify`; labels the namespace `pod-security.kubernetes.io/enforce=restricted`; `kind load docker-image opsdesk:dev`; `helm upgrade --install --atomic --wait` with `values-dev.yaml`, `route.enabled=false`, `image.repository=opsdesk`, `image.tag=dev`, `image.pullPolicy=Never`; no local kubectl/helm needed: kubectl via `docker exec opsdesk-verify-control-plane kubectl`, helm via `alpine/helm:4.3.1` with `--network host` and the file from `kind get kubeconfig`; the app is reached through a NodePort (`--set service.type=NodePort,service.nodePort=30080`, add these keys to the chart in this task with a unit test) and the kind `extraPortMappings` 30080 -> `127.0.0.1:18080`; `tools/smoke-test.sh http://127.0.0.1:18080`; runs `helm upgrade` a second time and smoke-tests again (Review Focus 2); deletes the cluster on exit)
- Modify: `docs/context/testing.md` (chart tests + kind verify)

- [ ] **Step 1:** `docker build -t opsdesk:dev . && tools/kind-verify.sh`. Expected: `SMOKE PASS` twice, cluster removed. A pod rejected by the `restricted` Pod Security level is a chart bug: fix it in the chart (with a unit test), not in the script.
- [ ] **Step 2: Commit** `test(helm): verify the chart on a local kind cluster`.

### Task 5: OpenShift deploy job and docs

**Files:**
- Modify: `.github/workflows/ci.yml`:
  - new job `helm` (always): `tools/helm-check.sh` (Docker is available on the runner).
  - `image` gets `needs: [verify, trivy-fs, helm]`.
  - `CAN_UPLOAD_SARIF` additionally requires `github.event_name == 'pull_request' || github.ref == 'refs/heads/main'`: feature-branch push runs skip the upload (the PR run uploads the same commit). Reason: the push run of PR #3 failed in its last step because the branch was deleted after the merge while the run was still uploading (`ref ... not found`).
  - new job `deploy-dev`: `needs: publish`, `if: github.event_name == 'push' && github.ref == 'refs/heads/main' && vars.OPENSHIFT_DEPLOY == 'true'`, `environment: { name: openshift-dev, url: ${{ steps.route.outputs.url }} }`, `concurrency: deploy-dev` (no cancel). Steps: checkout; install `oc` 5.0.1 from `https://mirror.openshift.com/pub/openshift-v4/clients/ocp/5.0.1/` (`openshift-client-linux.tar.gz` + `sha256sum.txt`, verify); `azure/setup-helm` (SHA-pinned, `version: v4.3.1`); `oc login --token="$OPENSHIFT_TOKEN" --server="$OPENSHIFT_SERVER"` from env (never echoed); `helm upgrade --install opsdesk deploy/helm/opsdesk -n "$OPENSHIFT_NAMESPACE" -f deploy/helm/opsdesk/values-dev.yaml --set image.tag=sha-${GITHUB_SHA::7} --atomic --wait --timeout 6m`; read the Route host (`oc get route opsdesk -o jsonpath='{.spec.host}'`) into `steps.route.outputs.url`; `tools/smoke-test.sh https://<host> 180`; on smoke failure `helm rollback opsdesk -n "$OPENSHIFT_NAMESPACE" --wait` and fail the job; always `helm history` + `oc get pods` for the log.
- Create: `deploy/openshift/README.md` (Developer Sandbox: sign up, namespace name; CI service account: `oc create sa github-deployer`, `oc policy add-role-to-user edit -z github-deployer`, long-lived token via a `kubernetes.io/service-account-token` Secret, `oc extract` / `oc get secret ... -o jsonpath` to read it once; GitHub: secrets `OPENSHIFT_SERVER`, `OPENSHIFT_TOKEN`, variables `OPENSHIFT_NAMESPACE`, `OPENSHIFT_DEPLOY=true`; manual deploy commands; how to roll back; Sandbox limits (30-day renewal, resource quota))
- Remove: `deploy/openshift/.gitkeep`, `deploy/helm/opsdesk/templates/.gitkeep`
- Modify: `docs/context/ops.md` (Kubernetes / OpenShift: real), `docs/context/cloud-mocks.md` (OpenShift row: chart real, deploy real when enabled, prod values mocked), `docs/context/status.md`, `docs/context/project-structure.md` (chart layout, `tools/helm-check.sh`, `tools/kind-verify.sh`), `CLAUDE.md` (commands)

- [ ] **Step 1:** actionlint clean; `tools/helm-check.sh` green; gitleaks on the working tree finds only `deploy/compose/.env`.
- [ ] **Step 2: Commit** `ci: add helm checks and gated openshift deploy job`, push. Expected CI on the branch: all jobs green, `deploy-dev` and `publish` skipped (not `main`).
- [ ] **Step 3:** Update status/docs, `python3 tools/context_check.py`, **commit** `docs: record phase 3 state`, push.
