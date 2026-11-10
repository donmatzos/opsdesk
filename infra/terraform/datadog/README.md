# Datadog monitors and dashboard (Terraform)

> MOCK: this project has no Datadog account. The code is complete and validated in CI
> (`terraform fmt -check`, `init -backend=false`, `validate`, tflint) but never planned or applied.

- `monitors.tf`: SEV1 incident opened, slow mean time to resolve (> 4 h), HTTP 5xx rate > 5 %; each links a runbook.
- `dashboard.tf`: opened incidents by severity, active incidents by state, MTTR, requests by outcome, latency.
- Metrics come from the app's Micrometer Datadog registry (`opsdesk.incidents.*`, `http.server.requests.*`),
  tagged via Unified Service Tagging (`service`, `env`, `version`, see the Helm chart's `datadog` values).

How it would be applied (credentials only from the environment, never in files):
```bash
export DD_API_KEY=... DD_APP_KEY=... DD_HOST=https://api.datadoghq.eu
terraform init            # with the remote backend from versions.tf enabled
terraform plan -var 'notify=@ops-oncall'
terraform apply -var 'notify=@ops-oncall'
```
