resource "datadog_dashboard" "opsdesk" {
  title       = "${var.service} (${var.env}): incidents and HTTP"
  description = "Managed by Terraform (infra/terraform/datadog). Business metrics from Micrometer."
  layout_type = "ordered"

  widget {
    timeseries_definition {
      title = "Opened incidents by severity"
      request {
        q            = "sum:opsdesk.incidents.opened{${local.scope}} by {severity}.as_count()"
        display_type = "bars"
      }
    }
  }

  widget {
    timeseries_definition {
      title = "Active incidents by state"
      request {
        q            = "max:opsdesk.incidents.active{${local.scope}} by {state}"
        display_type = "line"
      }
    }
  }

  widget {
    timeseries_definition {
      title = "Mean time to resolve (ms)"
      request {
        q            = "sum:opsdesk.incidents.time.to.resolve.sum{${local.scope}} by {severity}.as_count() / sum:opsdesk.incidents.time.to.resolve.count{${local.scope}} by {severity}.as_count()"
        display_type = "line"
      }
    }
  }

  widget {
    timeseries_definition {
      title = "Requests by outcome"
      request {
        q            = "sum:http.server.requests.count{${local.scope}} by {outcome}.as_count()"
        display_type = "bars"
      }
    }
  }

  widget {
    timeseries_definition {
      title = "Request latency (max, ms)"
      request {
        q            = "max:http.server.requests.max{${local.scope}} by {uri}"
        display_type = "line"
      }
    }
  }
}
