# MOCK: Azure OpenAI + AI Search infrastructure — no Azure account; validated in CI only, see docs/context/cloud-mocks.md
plugin "terraform" {
  enabled = true
  preset  = "recommended"
}

plugin "azurerm" {
  enabled = true
  version = "0.33.0"
  source  = "github.com/terraform-linters/tflint-ruleset-azurerm"
}
