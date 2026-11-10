#!/usr/bin/env bash
# Local verification of the Helm chart on a throwaway kind cluster (not part of CI; CI deploys to OpenShift).
# Enforces the "restricted" Pod Security level, installs with values-dev.yaml, smoke-tests, upgrades a second
# time and checks the generated DB password survived. Needs only Docker.
# Usage: tools/kind-verify.sh [image]     (default opsdesk:dev, build it first: docker build -t opsdesk:dev .)
# KEEP_CLUSTER=1 keeps the cluster after a failure for debugging (docker exec opsdesk-verify-control-plane kubectl ...).
set -euo pipefail

image=${1:-opsdesk:dev}
root=$(cd "$(dirname "$0")/.." && pwd)
kind_version=v0.33.0
cache=${XDG_CACHE_HOME:-$HOME/.cache}/opsdesk
kind=${cache}/kind-${kind_version}
cluster=opsdesk-verify
namespace=opsdesk
kubeconfig=${cache}/kubeconfig-${cluster}
host_port=18080

mkdir -p "${cache}"
if [[ ! -x ${kind} ]]; then
  base=https://github.com/kubernetes-sigs/kind/releases/download/${kind_version}
  curl -fsSL -o "${kind}.tmp" "${base}/kind-linux-amd64"
  expected=$(curl -fsSL "${base}/kind-linux-amd64.sha256sum" | awk '{print $1}')
  echo "${expected}  ${kind}.tmp" | sha256sum -c --quiet - || { rm -f "${kind}.tmp"; echo "kind checksum mismatch" >&2; exit 1; }
  chmod +x "${kind}.tmp" && mv "${kind}.tmp" "${kind}"
fi

cleanup() { "${kind}" delete cluster --name "${cluster}" >/dev/null 2>&1 || true; rm -f "${kubeconfig}"; }
on_exit() {
  local rc=$?
  if (( rc != 0 )) && [[ -n ${KEEP_CLUSTER:-} ]]; then
    echo "KEEP_CLUSTER set: cluster ${cluster} kept for debugging (kubeconfig: ${kubeconfig})" >&2
  else
    cleanup
  fi
}
trap on_exit EXIT
cleanup

"${kind}" create cluster --name "${cluster}" --wait 120s --config - <<YAML
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    extraPortMappings:
      - { containerPort: 30080, hostPort: ${host_port}, listenAddress: 127.0.0.1 }
YAML
"${kind}" get kubeconfig --name "${cluster}" > "${kubeconfig}"
"${kind}" load docker-image "${image}" --name "${cluster}"

kubectl() { docker exec "${cluster}-control-plane" kubectl "$@"; }
helm() {
  docker run --rm --network host -v "${root}:/apps:ro" -w /apps \
    -v "${kubeconfig}:/root/.kube/config:ro" alpine/helm:4.3.1 "$@"
}

kubectl create namespace "${namespace}"
kubectl label namespace "${namespace}" \
  pod-security.kubernetes.io/enforce=restricted pod-security.kubernetes.io/enforce-version=latest

install() {
  # The app NetworkPolicy admits the OpenShift router only; NodePort traffic from the host is not a pod, so the
  # app policy is off here. The database policy (only app pods may connect) stays on and is exercised.
  helm upgrade --install opsdesk deploy/helm/opsdesk -n "${namespace}" \
    -f deploy/helm/opsdesk/values-dev.yaml \
    --set route.enabled=false --set networkPolicy.enabled=false \
    --set service.type=NodePort --set service.nodePort=30080 \
    --set image.repository="${image%:*}" --set image.tag="${image##*:}" --set image.pullPolicy=Never \
    --rollback-on-failure --wait --timeout 5m
}
db_password_hash() {
  kubectl get secret opsdesk-db -n "${namespace}" -o jsonpath='{.data.password}' | sha256sum | cut -c1-16
}

echo "== install"
install
"${root}/tools/smoke-test.sh" "http://127.0.0.1:${host_port}" 120
first=$(db_password_hash)

echo "== upgrade (must keep the generated DB password)"
install
"${root}/tools/smoke-test.sh" "http://127.0.0.1:${host_port}" 120
second=$(db_password_hash)
[[ ${first} == "${second}" ]] || { echo "DB password changed on upgrade" >&2; exit 1; }
echo "ok  DB password kept across upgrade"

kubectl get pods -n "${namespace}" -o wide
echo "KIND VERIFY PASS"
