# MOCK: Azure OpenAI + AI Search infrastructure — no Azure account; validated in CI only (fmt, validate, tflint), never planned or applied, see docs/context/cloud-mocks.md
locals {
  base_name = "${var.name_prefix}-${var.environment}"
  tags = {
    app          = "opsdesk"
    env          = var.environment
    "managed-by" = "terraform"
  }
}
