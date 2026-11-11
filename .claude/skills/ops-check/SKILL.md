---
name: ops-check
description: Use before committing changes to CI/CD files, Helm, Terraform, Ansible, GitLab or Jenkins templates, runbooks, docs or context files in opsdesk. Runs the repository's offline checks and reports every failure with its cause.
---

# ops-check

Run the checks that match what you changed, read each result, and report failures with the output that explains them. Do not claim success from a diff that looks right; a check passes only when you saw it pass.

## Run
1. Always: `python3 tools/context_check.py` (context files: at most 100 lines and 6 KB each) and `python3 tools/docs_check.py` (README, runbook and context links, runbooks named by the Datadog monitors, mock inventory).
2. Docs or guide pages: `python3 tools/guide_check.py`; styles or tokens: `python3 tools/contrast_check.py`.
3. Helm chart: `tools/helm-check.sh` (Docker only: lint, unit tests, schema).
4. GitLab template, Jenkinsfile, Ansible playbook: `tools/ci-templates-check.sh all` (Docker only). One target: `gitlab`, `jenkins` or `ansible`.
5. Workflows: `docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:1.7.12`.
6. Terraform (`infra/terraform/<dir>`): `terraform fmt -check`, `init -backend=false`, `validate`, then tflint, as the `terraform` job in `ci.yml` does.
7. Release files: `tools/release-check.sh vX.Y.Z` fails until pom and chart carry that version (expected on a SNAPSHOT).
8. Last: `./mvnw -B -ntp verify` must be green before a commit.

## Report
List each command with pass or fail. For a failure quote the first lines that name the cause and the file, and say what you changed. If a check could not run (no Docker, no network), say so instead of skipping it silently.

## Never
- Add secrets, not even fake-looking ones. Run `gitleaks dir .`: only `deploy/compose/.env` may show up (git-ignored).
- Suppress a finding or add `noqa` before the owner has seen it (see `docs/context/conventions.md`).
