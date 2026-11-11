# MOCK: Ansible Tower/AWX — no instance exists; validated in CI only (fmt, validate, tflint), never planned or applied, see docs/context/cloud-mocks.md
terraform {
  required_version = ">= 1.17"

  required_providers {
    awx = {
      source  = "denouche/awx"
      version = "~> 0.29"
    }
  }

  # MOCK: no AWX and no remote state. A team setup would keep state remotely, e.g.:
  # backend "s3" {
  #   bucket       = "opsdesk-terraform-state"
  #   key          = "awx/terraform.tfstate"
  #   region       = "eu-central-1"
  #   use_lockfile = true
  # }
}
