locals {
  # Datadog lowercases tag values; Micrometer tags arrive as e.g. severity:sev1.
  scope = "service:${var.service},env:${var.env}"
  tags  = ["service:${var.service}", "env:${var.env}", "team:ops", "managed-by:terraform"]
}
