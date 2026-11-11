# MOCK: Ansible Tower/AWX — no instance exists; validated in CI only (fmt, validate, tflint), never planned or applied, see docs/context/cloud-mocks.md
data "awx_organization" "this" {
  name = var.organization
}

resource "awx_project" "opsdesk" {
  name                 = "opsdesk"
  organization_id      = data.awx_organization.this.id
  scm_type             = "git"
  scm_url              = var.scm_url
  scm_branch           = var.scm_branch
  scm_update_on_launch = true
}

resource "awx_inventory" "stage" {
  for_each        = local.stages
  name            = "opsdesk-${each.key}"
  organization_id = data.awx_organization.this.id
}

# The playbook talks to the cluster API from the controller, so every inventory has only localhost.
resource "awx_host" "localhost" {
  for_each     = local.stages
  name         = "localhost"
  inventory_id = awx_inventory.stage[each.key].id
  variables    = jsonencode({ ansible_connection = "local" })
}

resource "awx_job_template" "deploy" {
  for_each                 = local.stages
  name                     = "opsdesk-deploy-${each.key}"
  description              = each.value.description
  job_type                 = "run"
  inventory_id             = awx_inventory.stage[each.key].id
  project_id               = awx_project.opsdesk.id
  playbook                 = "infra/ansible/playbooks/deploy.yml"
  ask_variables_on_launch  = true
  ask_inventory_on_launch  = false
  ask_credential_on_launch = false
}

resource "awx_job_template_credential" "openshift" {
  for_each        = local.stages
  job_template_id = awx_job_template.deploy[each.key].id
  credential_id   = var.openshift_credential_ids[each.key]
}
