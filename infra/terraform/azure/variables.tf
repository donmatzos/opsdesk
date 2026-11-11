# MOCK: Azure OpenAI + AI Search infrastructure — no Azure account; validated in CI only (fmt, validate, tflint), never planned or applied, see docs/context/cloud-mocks.md
variable "location" {
  description = "Azure region for all resources."
  type        = string
  default     = "swedencentral"
}

variable "name_prefix" {
  description = "Prefix of all resource names (lowercase letters and digits, max 12)."
  type        = string
  default     = "opsdesk"

  validation {
    condition     = can(regex("^[a-z][a-z0-9]{1,11}$", var.name_prefix))
    error_message = "name_prefix must be 2-12 lowercase letters or digits and start with a letter."
  }
}

variable "environment" {
  description = "Stage name, used in resource names and tags."
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "test", "prod"], var.environment)
    error_message = "environment must be dev, test or prod."
  }
}

variable "openai_model_name" {
  description = "Azure OpenAI model to deploy. Only a default, never applied; check Azure's model retirement table before a real apply."
  type        = string
  default     = "gpt-4.1-mini"
}

variable "openai_model_version" {
  description = "Version of the Azure OpenAI model."
  type        = string
  default     = "2025-04-14"
}

variable "openai_capacity" {
  description = "Capacity of the deployment in thousands of tokens per minute."
  type        = number
  default     = 10

  validation {
    condition     = var.openai_capacity >= 1 && var.openai_capacity <= 100
    error_message = "openai_capacity must be between 1 and 100."
  }
}

variable "search_index_name" {
  description = "Name of the search index; must match search-index.json and AZURE_SEARCH_INDEX."
  type        = string
  default     = "opsdesk-runbooks"
}
