# Azure OpenAI and AI Search (Terraform)

> MOCK: this project has no Azure account. The code is complete and validated in CI
> (`terraform fmt -check`, `init -backend=false`, `validate`, tflint) but never planned or applied.

What it describes: a resource group, an Azure AI Search service (`basic`), an Azure OpenAI account and one chat
deployment. It backs the app's profile `azure` (classes `AzureOpenAiLlmClient`, `AzureAiSearchRetriever`,
`AzureAiSearchIndexer`); without the profile the app uses a BM25 index and a mock model.

Credentials only from the environment, never in files: `ARM_SUBSCRIPTION_ID`, `ARM_TENANT_ID`, and OIDC or a
Managed Identity in CI. The outputs contain no keys. Production would use Entra ID RBAC and private endpoints
instead of keys and public access (see comments in `main.tf`).

How the outputs map to the app's six variables:

| App variable | Source |
|---|---|
| `AZURE_OPENAI_ENDPOINT` | output `openai_endpoint` |
| `AZURE_OPENAI_DEPLOYMENT` | output `openai_deployment` |
| `AZURE_SEARCH_ENDPOINT` | output `search_endpoint` |
| `AZURE_SEARCH_INDEX` | output `search_index_name` |
| `AZURE_OPENAI_API_KEY`, `AZURE_SEARCH_API_KEY` | from the portal or `az`, into a Secret; never into Terraform |

The index is created from `search-index.json` (it must match what the app reads and writes; a test checks that):
```bash
curl -X PUT "$AZURE_SEARCH_ENDPOINT/indexes/opsdesk-runbooks?api-version=2024-07-01" \
  -H "Content-Type: application/json" -H "api-key: $AZURE_SEARCH_API_KEY" -d @search-index.json
```
Then start the app with `SPRING_PROFILES_ACTIVE=azure` and `opsdesk.ai.azure.search.index-on-startup=true` once to
upload the runbooks.
