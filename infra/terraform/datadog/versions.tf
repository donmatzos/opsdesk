terraform {
  required_version = ">= 1.17"

  required_providers {
    datadog = {
      source  = "DataDog/datadog"
      version = "~> 4.30"
    }
  }

  # MOCK: no Datadog account and no remote state. A team setup would keep state remotely, e.g.:
  # backend "s3" {
  #   bucket       = "opsdesk-terraform-state"
  #   key          = "datadog/terraform.tfstate"
  #   region       = "eu-central-1"
  #   use_lockfile = true
  # }
}
