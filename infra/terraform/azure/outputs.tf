# MOCK: Azure OpenAI + AI Search infrastructure — no Azure account; validated in CI only (fmt, validate, tflint), never planned or applied, see docs/context/cloud-mocks.md
# Endpoints and names only; no keys (read them from the portal or az CLI when needed, never store them in state outputs).
output "openai_endpoint" {
  description = "Value for AZURE_OPENAI_ENDPOINT."
  value       = azurerm_cognitive_account.openai.endpoint
}

output "openai_deployment" {
  description = "Value for AZURE_OPENAI_DEPLOYMENT."
  value       = azurerm_cognitive_deployment.chat.name
}

output "search_endpoint" {
  description = "Value for AZURE_SEARCH_ENDPOINT."
  value       = "https://${azurerm_search_service.this.name}.search.windows.net"
}

output "search_index_name" {
  description = "Value for AZURE_SEARCH_INDEX."
  value       = var.search_index_name
}
