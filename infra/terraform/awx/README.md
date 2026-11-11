# AWX job templates (Terraform)

> MOCK: this project has no Ansible Tower / AWX instance. The code is complete and validated in CI
> (`terraform fmt -check`, `init -backend=false`, `validate`, tflint) but never planned or applied.

It describes, per stage (`dev`, `prod`): an AWX project (this Git repository), an inventory with `localhost`, and a job template
that runs `infra/ansible/playbooks/deploy.yml`, asks for the extra variable `opsdesk_image_tag` on launch and has the OpenShift
credential attached. The production template is launched by hand.

Host and token only from the environment, never in files: `AWX_HOSTNAME`, `AWX_TOKEN`. The OpenShift credentials are created in
AWX (type "OpenShift or Kubernetes API Bearer Token"); only their numeric ids are passed in, through `openshift_credential_ids`:

```bash
export AWX_HOSTNAME=https://awx.example.com AWX_TOKEN=...   # from your AWX user, never committed
terraform init            # with the remote backend from versions.tf enabled
terraform plan -var 'openshift_credential_ids={dev=3,prod=4}'
```
