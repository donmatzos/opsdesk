# MOCK: Azure OpenAI + AI Search infrastructure — no Azure account; validated in CI only (fmt, validate, tflint), never planned or applied, see docs/context/cloud-mocks.md
resource "azurerm_resource_group" "this" {
  name     = "rg-${local.base_name}"
  location = var.location
  tags     = local.tags
}

# Search for the runbooks. Keys (local authentication) keep the showcase simple; production uses Entra ID RBAC
# (local_authentication_enabled = false), a private endpoint and public_network_access_enabled = false.
resource "azurerm_search_service" "this" {
  name                          = "${local.base_name}-search"
  resource_group_name           = azurerm_resource_group.this.name
  location                      = azurerm_resource_group.this.location
  sku                           = "basic"
  local_authentication_enabled  = true
  authentication_failure_mode   = "http403"
  public_network_access_enabled = true
  tags                          = local.tags
}

# Azure OpenAI account; same note: production uses Entra ID RBAC and a private endpoint instead of keys.
resource "azurerm_cognitive_account" "openai" {
  name                  = "${local.base_name}-openai"
  resource_group_name   = azurerm_resource_group.this.name
  location              = azurerm_resource_group.this.location
  kind                  = "OpenAI"
  sku_name              = "S0"
  custom_subdomain_name = "${local.base_name}-openai"
  tags                  = local.tags
}

resource "azurerm_cognitive_deployment" "chat" {
  name                 = "chat"
  cognitive_account_id = azurerm_cognitive_account.openai.id

  model {
    format  = "OpenAI"
    name    = var.openai_model_name
    version = var.openai_model_version
  }

  sku {
    name     = "GlobalStandard"
    capacity = var.openai_capacity
  }
}
