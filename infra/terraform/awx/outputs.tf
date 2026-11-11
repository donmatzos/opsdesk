# MOCK: Ansible Tower/AWX — no instance exists; validated in CI only (fmt, validate, tflint), never planned or applied, see docs/context/cloud-mocks.md
output "job_template_names" {
  description = "AWX job templates that run infra/ansible/playbooks/deploy.yml, per stage."
  value       = { for stage, template in awx_job_template.deploy : stage => template.name }
}
