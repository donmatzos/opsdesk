# Deploying opsdesk to OpenShift

CI deploys `main` to an OpenShift namespace with Helm (`.github/workflows/ci.yml`, job `deploy-dev`). The job is
off until you switch it on. Any OpenShift 4.x/5.x namespace works; the free **Red Hat Developer Sandbox** is enough.

## 1. Get a namespace (Developer Sandbox)
1. Open https://developers.redhat.com/developer-sandbox and start the sandbox (Red Hat account, phone verification).
2. Your namespace is `<username>-dev`. In the web console: user menu -> **Copy login command** -> **Display token**.
   Note the `--server=https://api....:6443` URL (this is `OPENSHIFT_SERVER`). The personal token expires quickly;
   CI uses a service account instead.

## 2. Service account for CI
```bash
oc login --token=<your personal token> --server=<server>
oc project <username>-dev
oc create serviceaccount github-deployer
oc policy add-role-to-user edit -z github-deployer

# Long-lived token bound to the service account (delete the Secret to revoke it).
oc apply -f - <<'YAML'
apiVersion: v1
kind: Secret
metadata:
  name: github-deployer-token
  annotations:
    kubernetes.io/service-account.name: github-deployer
type: kubernetes.io/service-account-token
YAML

# Print it once and paste it straight into GitHub; do not store it anywhere else.
oc get secret github-deployer-token -o jsonpath='{.data.token}' | base64 -d; echo
```
If the cluster does not issue long-lived tokens, use `oc create token github-deployer --duration=720h` and renew it.

## 3. GitHub settings (repository -> Settings -> Secrets and variables -> Actions)
| Kind | Name | Value |
|---|---|---|
| Secret | `OPENSHIFT_SERVER` | API URL from step 1 |
| Secret | `OPENSHIFT_TOKEN` | service account token from step 2 |
| Variable | `OPENSHIFT_NAMESPACE` | `<username>-dev` |
| Variable | `PUBLISH_IMAGE` | `true` (the cluster pulls `ghcr.io/donmatzos/opsdesk`) |
| Variable | `OPENSHIFT_DEPLOY` | `true` |

The GHCR package must be **public** (GitHub -> Packages -> opsdesk -> Package settings), so the cluster can pull
without credentials.

## 4. What the job does
`oc login` -> `helm upgrade --install opsdesk deploy/helm/opsdesk -f values-dev.yaml --set image.tag=sha-<commit>
--rollback-on-failure --wait` -> `tools/smoke-test.sh https://<route>` -> on smoke failure `helm rollback` to the
previous release. The GitHub environment `openshift-dev` shows the deployed URL.

The dev stage brings its own PostgreSQL (`devDatabase`, Red Hat sclorg image) with a password generated in the
cluster on first install and kept on upgrades (annotation `helm.sh/resource-policy: keep`).

## 5. By hand
```bash
helm upgrade --install opsdesk deploy/helm/opsdesk -n <ns> -f deploy/helm/opsdesk/values-dev.yaml \
  --set image.tag=sha-<commit> --rollback-on-failure --wait
helm history opsdesk -n <ns>
helm rollback opsdesk <revision> -n <ns> --wait
helm uninstall opsdesk -n <ns>   # keeps the DB Secret and volume; delete them explicitly to reset
```

## Notes
- Pods run under the `restricted-v2` SCC: random UID, no privilege escalation, read-only root filesystem.
- The app NetworkPolicy admits the OpenShift router (`policy-group.network.openshift.io/ingress`) and pods of the
  same namespace; the database admits only the app pods.
- Sandbox limits (renewal period, quotas, idling of inactive workloads) change over time; check the Sandbox page.
