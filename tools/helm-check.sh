#!/usr/bin/env bash
# Validate the Helm chart with Docker only (no local helm/kubectl): lint, unit tests, schema check of the
# rendered manifests (incl. the OpenShift Route CRD). Used locally and in CI. Usage: tools/helm-check.sh
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
chart=deploy/helm/opsdesk
helm_image=alpine/helm:4.3.1
unittest_image=helmunittest/helm-unittest:4.3.0-1.2.1
kubeconform_image=ghcr.io/yannh/kubeconform:v0.8.0
crd_catalog=https://raw.githubusercontent.com/datreeio/CRDs-catalog/main
openshift_schemas="${crd_catalog}/openshift/v4.15-strict/{{.ResourceKind}}_{{.Group}}_{{.ResourceAPIVersion}}.json"
crd_schemas="${crd_catalog}/{{.Group}}/{{.ResourceKind}}_{{.ResourceAPIVersion}}.json"

# The default values need a database Secret by design (no silent fallback); render them like a real install would.
default_db=(--set database.existingSecret=opsdesk-db --set database.url=jdbc:postgresql://db:5432/opsdesk)

helm() { docker run --rm -v "${root}:/apps" -w /apps "${helm_image}" "$@"; }

variants=("default")
for f in "${root}/${chart}"/values-*.yaml; do [[ -e $f ]] && variants+=("$(basename "$f")"); done

args_for() {
  if [[ $1 == default ]]; then printf '%s\n' "${default_db[@]}"; else printf '%s\n' -f "${chart}/$1"; fi
}

for v in "${variants[@]}"; do
  mapfile -t args < <(args_for "$v")
  echo "== helm lint (${v})"
  helm lint --strict "${chart}" "${args[@]}"
done

echo "== helm unittest"
docker run --rm -v "${root}:/apps" -w /apps "${unittest_image}" --strict "${chart}"

for v in "${variants[@]}"; do
  mapfile -t args < <(args_for "$v")
  echo "== kubeconform (${v})"
  helm template opsdesk "${chart}" "${args[@]}" \
    | docker run --rm -i "${kubeconform_image}" -strict -summary \
        -schema-location default -schema-location "${openshift_schemas}" -schema-location "${crd_schemas}" -
done

echo "HELM CHECK PASS"
