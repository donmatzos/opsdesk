# MOCK: Azure OpenAI + AI Search infrastructure — no Azure account; validated in CI only (fmt, validate, tflint), never planned or applied, see docs/context/cloud-mocks.md
terraform {
  required_version = ">= 1.17"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 5.13"
    }
  }

  # MOCK: no Azure account and no remote state. A team setup would keep state remotely, e.g.:
  # backend "azurerm" {
  #   resource_group_name  = "opsdesk-tfstate"
  #   storage_account_name = "opsdesktfstate"
  #   container_name       = "tfstate"
  #   key                  = "azure/terraform.tfstate"
  # }
}
