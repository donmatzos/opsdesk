# MOCK: Ansible Tower/AWX — no instance exists; validated in CI only (fmt, validate, tflint), never planned or applied, see docs/context/cloud-mocks.md
variable "organization" {
  description = "Name of the AWX organization that owns the objects."
  type        = string
  default     = "Default"
}

variable "scm_url" {
  description = "Git repository that holds the playbook."
  type        = string
  default     = "https://github.com/donmatzos/opsdesk.git"
}

variable "scm_branch" {
  description = "Branch (or tag) AWX checks out."
  type        = string
  default     = "main"
}

variable "openshift_credential_ids" {
  description = "AWX credential id per stage (type \"OpenShift or Kubernetes API Bearer Token\"). Created in AWX by hand; only the numeric id is referenced here."
  type        = map(number)

  validation {
    condition     = alltrue([for stage in ["dev", "prod"] : contains(keys(var.openshift_credential_ids), stage)])
    error_message = "openshift_credential_ids needs an entry for dev and for prod."
  }
}
