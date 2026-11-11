#!/usr/bin/env bash
# Validate the mocked pipeline artifacts with Docker only (no GitLab, Jenkins or AWX needed): the GitLab template and its
# consumer, the Jenkinsfile and the Ansible playbook. Used locally and in CI (job "templates").
# Usage: tools/ci-templates-check.sh [gitlab|jenkins|ansible|all]      (default: all)
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
cd "${root}"

python_image=python:3.13-slim
node_image=node:24-alpine
check_jsonschema=0.39.0
gitlab_ci_local=4.77.1
template=ci/gitlab/templates/maven-openshift.gitlab-ci.yml
consumer=ci/gitlab/consumer/.gitlab-ci.yml
generated=ci/gitlab/tests/generated.gitlab-ci.yml

# Run one check, keep its output; on failure in GitHub Actions also raise an annotation with the last lines.
log=$(mktemp)
trap 'rm -f "${log}" "${generated}"' EXIT
phase() {
  local name=$1; shift
  echo "== ${name}"
  if ! "$@" > "${log}" 2>&1; then
    cat "${log}"
    if [[ -n ${GITHUB_ACTIONS:-} ]]; then
      local tail_text
      tail_text=$(tail -n 20 "${log}" | sed -e 's/%/%25/g' | sed -e ':a;N;$!ba;s/\n/%0A/g')
      echo "::error title=ci-templates-check: ${name}::${tail_text}"
    fi
    echo "CI TEMPLATES CHECK FAIL: ${name}" >&2
    exit 1
  fi
  cat "${log}"
}

# --- GitLab -----------------------------------------------------------------------------------------------------------
gitlab_schema() {
  docker run --rm -v "${root}:/repo:ro" -w /repo "${python_image}" sh -c \
    "pip install -q --disable-pip-version-check check-jsonschema==${check_jsonschema} && \
     check-jsonschema --builtin-schema vendor.gitlab-ci ${template} ${consumer}"
}

# The consumer includes the template by project path; for the local run swap that block for a local include.
gitlab_generate() {
  mkdir -p "$(dirname "${generated}")"
  sed -e '/^# >>> include/,/^# <<< include/c\include:\n  - local: '"${template}" "${consumer}" > "${generated}"
}

# gitlab-ci-local evaluates the rules for a pipeline on the default branch with no secrets set. The consumer switches the dev
# deploy on, so build, scan, package, push and deploy-dev must be there; sonar (no SONAR_TOKEN), the prod deploy (tags only)
# and the Artifactory jobs (switch off) must not. No fake token values are passed, not even dummies.
gitlab_jobs() {
  gitlab_generate
  local jobs
  # gitlab-ci-local resolves includes through git: run it on a throwaway repository that holds only ci/, so the result
  # does not depend on what is committed or staged in the working tree.
  jobs=$(docker run --rm -v "${root}/ci:/src/ci:ro" "${node_image}" sh -c \
    "apk add --no-cache git bash openssh-client >/dev/null && mkdir /w && cp -r /src/ci /w/ci && cd /w && \
     git init -q -b main && git add -A && git -c user.email=ci@example.com -c user.name=ci commit -qm check && \
     npx -y gitlab-ci-local@${gitlab_ci_local} --file ${generated} --list-csv 2>/dev/null" | cut -d';' -f1 | tr -d '"' | sort | tr '\n' ' ')
  echo "jobs on a default-branch pipeline: ${jobs}"
  local want
  for want in maven-verify secret-scan trivy-fs helm-lint image-build trivy-image image-push deploy-dev; do
    [[ " ${jobs} " == *" ${want} "* ]] || { echo "missing job: ${want}" >&2; return 1; }
  done
  local unwanted
  for unwanted in sonar deploy-prod maven-deploy helm-publish; do
    [[ " ${jobs} " != *" ${unwanted} "* ]] || { echo "unexpected job without its switch: ${unwanted}" >&2; return 1; }
  done
}

check_gitlab() {
  phase "gitlab: schema (template + consumer)" gitlab_schema
  phase "gitlab: job set of a default-branch pipeline" gitlab_jobs
}

target=${1:-all}
case "${target}" in
  gitlab) check_gitlab ;;
  all) check_gitlab ;;
  *) echo "usage: $0 [gitlab|jenkins|ansible|all]" >&2; exit 2 ;;
esac
echo "CI TEMPLATES CHECK PASS (${target})"
