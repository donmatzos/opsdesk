# Cloud mocks (no cloud account exists)
Rule: a mock must be **honest and realistic** — written the way the real integration would be, clearly marked, off by default, and never pretending to have run against the cloud.
| Real thing | How it appears | Switch |
|---|---|---|
| Azure OpenAI | `AzureOpenAiLlmClient` behind `LlmClient`; default `MockLlmClient` | profile `azure` (fails fast without env vars) |
| Azure AI Search | `AzureAiSearchRetriever` behind `Retriever`; default in-memory index | profile `azure` |
| Azure resources | `infra/terraform/azure` (validate only, remote backend commented) | never applied |
| OpenShift | Real chart + real CI deploy (`deploy-dev`) when switched on; prod values are a mock | `OPENSHIFT_DEPLOY=true` + secrets `OPENSHIFT_SERVER`/`OPENSHIFT_TOKEN`, var `OPENSHIFT_NAMESPACE` |
| Datadog | Micrometer Datadog registry (profile `datadog`, key only from env `DATADOG_API_KEY`, `DatadogKeyCheck` fails fast); chart `datadog.enabled` (Unified Service Tagging, agent host, key from Secret); Terraform monitors + dashboard | profile `datadog`; chart value; Terraform validate only |
| Artifactory | commented `distributionManagement`/Helm push; real: GHCR/GitHub Packages | - |
| Ansible Tower | playbook + `awx` provider job template (commented) | - |
Marker comment: `MOCK: <thing> — <why>`. README lists every mock.
