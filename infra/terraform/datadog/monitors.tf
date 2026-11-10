# Metrics come from the app's Micrometer Datadog registry (profile "datadog"); timers arrive in milliseconds,
# tagged service/env/version by the app itself (it posts to the API directly, no agent enrichment).

resource "datadog_monitor" "sev1_opened" {
  name    = "[${var.env}] ${var.service}: SEV1 incident opened"
  type    = "query alert"
  query   = "sum(last_5m):sum:opsdesk.incidents.opened{${local.scope},severity:sev1}.as_count() > 0"
  message = <<-EOT
    A SEV1 incident was opened in ${var.service} (${var.env}).
    Runbook: ${var.runbook_base_url}/sev1-incident.md
    ${var.notify}
  EOT

  monitor_thresholds {
    critical = 0
  }

  notify_no_data      = false
  require_full_window = false
  tags                = local.tags
}

resource "datadog_monitor" "slow_resolution" {
  name = "[${var.env}] ${var.service}: incidents take too long to resolve"
  type = "query alert"
  # Real mean over the window: sum / count. (Averaging the per-step .avg would mostly average zeros.)
  query   = "sum(last_1d):sum:opsdesk.incidents.time.to.resolve.sum{${local.scope}}.as_count() / sum:opsdesk.incidents.time.to.resolve.count{${local.scope}}.as_count() > 14400000"
  message = <<-EOT
    Mean time to resolve is above 4 h over the last day in ${var.service} (${var.env}).
    Runbook: ${var.runbook_base_url}/postmortem-template.md
    ${var.notify}
  EOT

  monitor_thresholds {
    warning  = 7200000
    critical = 14400000
  }

  notify_no_data = false
  tags           = local.tags
}

resource "datadog_monitor" "http_errors" {
  name    = "[${var.env}] ${var.service}: HTTP 5xx rate above 5 %"
  type    = "query alert"
  query   = "sum(last_10m):100 * sum:http.server.requests.count{${local.scope},outcome:server_error}.as_count() / sum:http.server.requests.count{${local.scope}}.as_count() > 5"
  message = <<-EOT
    More than 5 % of the requests to ${var.service} (${var.env}) failed with 5xx in the last 10 minutes.
    Runbook: ${var.runbook_base_url}/http-errors.md
    ${var.notify}
  EOT

  monitor_thresholds {
    warning  = 2
    critical = 5
  }

  notify_no_data = false
  tags           = local.tags
}
