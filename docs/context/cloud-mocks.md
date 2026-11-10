# Cloud mocks (no cloud account exists)
Rule: a mock must be **honest and realistic** — written the way the real integration would be, clearly marked, off by default, and never pretending to have run against the cloud.
| Real thing | How it appears | Switch |
|---|---|---|
| Azure OpenAI | `AzureOpenAiLlmClient` behind `LlmClient`; default `MockLlmClient` | profile `azure` (fails fast without env vars) |
| Azure AI Search | `AzureAiSearchRetriever` behind `Retriever`; default in-memory index | profile `azure` |
| Azure resources | `infra/terraform/azure` (validate only, remote backend commented) | never applied |
| OpenShift | Route template; CI deploy job via `oc` + Helm, skipped without secrets | `route.enabled`; secrets `OPENSHIFT_SERVER`/`OPENSHIFT_TOKEN` |
| Datadog | Micrometer Datadog registry; Terraform monitors | profile `datadog`; validate only |
| Artifactory | commented `distributionManagement`/Helm push; real: GHCR/GitHub Packages | - |
| Ansible Tower | playbook + `awx` provider job template (commented) | - |
Marker comment: `MOCK: <thing> — <why>`. README lists every mock.
