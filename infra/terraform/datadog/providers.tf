# Credentials only from the environment: DD_API_KEY, DD_APP_KEY, DD_HOST (e.g. https://api.datadoghq.eu).
# Nothing here or in tfvars; CI only runs fmt/validate/tflint, never plan/apply (MOCK: no account).
provider "datadog" {}
