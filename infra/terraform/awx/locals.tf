# MOCK: Ansible Tower/AWX — no instance exists; validated in CI only (fmt, validate, tflint), never planned or applied, see docs/context/cloud-mocks.md
locals {
  stages = {
    dev = {
      description = "Deploy opsdesk to the dev namespace (ask for the image tag on launch)."
    }
    prod = {
      # MOCK: production. Launched manually; the Helm values and namespace come from the prod inventory.
      description = "Deploy opsdesk to production (manual launch only)."
    }
  }
}
