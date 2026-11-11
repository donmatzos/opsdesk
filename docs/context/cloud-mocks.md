# Cloud mocks (no cloud account exists)
Rule: a mock must be **honest and realistic** — written the way the real integration would be, clearly marked, off by default, and never pretending to have run against the cloud.
| Real thing | How it appears | Switch |
|---|---|---|
| Azure OpenAI | `AzureOpenAiLlmClient` (Azure OpenAI v1 REST, JDK `HttpClient`) behind `LlmClient`; default `MockLlmClient` (answers start `MOCK answer`, response `mock: true`) | profile `azure`; fails fast naming all missing of `AZURE_OPENAI_{ENDPOINT,API_KEY,DEPLOYMENT}`, `AZURE_SEARCH_{ENDPOINT,API_KEY,INDEX}` |
| Azure AI Search | `AzureAiSearchRetriever` + `AzureAiSearchIndexer` (REST `2024-07-01`) behind `Retriever`; default `RunbookIndex` (BM25, in memory); index `infra/terraform/azure/search-index.json` | profile `azure`; upload at start only with `opsdesk.ai.azure.search.index-on-startup=true` |
| Azure resources | `infra/terraform/azure` (validate only, remote backend commented, no keys in outputs) | never applied |
| OpenShift | Real chart + real CI deploy (`deploy-dev`) when switched on; prod values are a mock | `OPENSHIFT_DEPLOY=true` + secrets `OPENSHIFT_SERVER`/`OPENSHIFT_TOKEN`, var `OPENSHIFT_NAMESPACE` |
| Datadog | Micrometer Datadog registry (profile `datadog`, key only from env `DATADOG_API_KEY`, `DatadogKeyCheck` fails fast); chart `datadog.enabled` (Unified Service Tagging, agent host, key from Secret); Terraform monitors + dashboard | profile `datadog`; chart value; Terraform validate only |
| Artifactory | Maven profile `artifactory` (`distributionManagement` on `artifactory.example.com`, credentials only from a settings.xml written by the pipeline); GitLab `maven-deploy`/`helm-publish` and Jenkins `Publish` use it. Real: GHCR/GitHub Releases | `-Partifactory` + `ARTIFACTORY_ENABLED=true` (GitLab) |
| Ansible Tower | playbook + `awx` provider job template (commented) | - |
Warning: `/api/assist` has no auth and no rate limit; with profile `azure` each call costs Azure requests. Close and/or throttle it before real use (Route/Ingress rule, auth, or rate limit).
Keys: `AzureSettings` holds no secret; `AzureCredentials` looks the two keys up in the environment (Secret in a cluster, `.env` locally) on each request and keeps no copy.
Marker comment: `MOCK: <thing> — <why>`. README lists every mock.
