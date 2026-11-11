# MOCK: Azure OpenAI + AI Search infrastructure — no Azure account; validated in CI only (fmt, validate, tflint), never planned or applied, see docs/context/cloud-mocks.md
# Subscription and authentication only from the environment: ARM_SUBSCRIPTION_ID, ARM_TENANT_ID and, in CI, OIDC
# (ARM_USE_OIDC) or a Managed Identity. Nothing here or in tfvars; no client secrets, ever.
provider "azurerm" {
  features {}
}
