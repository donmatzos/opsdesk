# Ansible deploy playbook (mocked)

> MOCK: there is no Ansible Tower / AWX instance. The playbook is syntax-checked and linted (`ansible-lint`, profile `production`)
> in CI by `tools/ci-templates-check.sh ansible`, and never run by CI.

`playbooks/deploy.yml` deploys the same Helm chart as the pipelines, with the same values files, and applies the same rule as the
`deploy-dev` job in `.github/workflows/ci.yml`: wait for the rollout (`atomic`), smoke-test the route with
`tools/smoke-test.sh`, and roll back to the previous revision if the smoke test fails, unless it was the first release.

| Path | Content |
|---|---|
| `playbooks/deploy.yml` | the deploy; needs `-e opsdesk_image_tag=<tag>` |
| `inventories/{dev,prod}/` | localhost inventory and the stage variables (namespace, values file) |
| `collections/requirements.yml` | `kubernetes.core`, pinned |
| `../terraform/awx/` | the AWX job templates that run this playbook (Terraform, validated only) |

## How AWX runs it
A job template per stage (see `infra/terraform/awx`) points at this playbook in the Git project, asks for `opsdesk_image_tag` on
launch and attaches an AWX credential of type "OpenShift or Kubernetes API Bearer Token". That credential injects
`K8S_AUTH_HOST` and `K8S_AUTH_API_KEY`; nothing secret is in the repository, and there is no vault file on purpose.

## Run it locally (against a cluster you are logged in to)
```bash
cd infra/ansible
ansible-galaxy collection install -r collections/requirements.yml -p collections
export K8S_AUTH_HOST=...   # cluster API URL, from `oc whoami --show-server`
export K8S_AUTH_API_KEY=$(oc whoami -t)
ansible-playbook playbooks/deploy.yml -i inventories/dev -e opsdesk_image_tag=sha-abc1234
```
Needs `helm`, `oc` and Python `kubernetes` on the machine.
