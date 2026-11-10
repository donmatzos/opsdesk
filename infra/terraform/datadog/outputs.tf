output "monitor_ids" {
  description = "IDs of the managed monitors."
  value = {
    sev1_opened     = datadog_monitor.sev1_opened.id
    slow_resolution = datadog_monitor.slow_resolution.id
    http_errors     = datadog_monitor.http_errors.id
  }
}

output "dashboard_url" {
  description = "Path of the dashboard in the Datadog UI."
  value       = datadog_dashboard.opsdesk.url
}
