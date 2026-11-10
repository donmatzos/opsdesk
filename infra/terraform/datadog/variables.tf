variable "env" {
  description = "Deployment environment, matches the DD_ENV / tags.datadoghq.com/env of the workload."
  type        = string
  default     = "dev"
}

variable "service" {
  description = "Service name, matches DD_SERVICE and the ECS service.name."
  type        = string
  default     = "opsdesk"
}

variable "notify" {
  description = "Datadog notification handle(s) for alerts, e.g. \"@ops-oncall\" or \"@slack-ops-alerts\"."
  type        = string
}

variable "runbook_base_url" {
  description = "Where the runbooks live; monitors link to them."
  type        = string
  default     = "https://github.com/donmatzos/opsdesk/tree/main/docs/runbooks"
}
