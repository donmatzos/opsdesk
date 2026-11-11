# MOCK: Ansible Tower/AWX — no instance exists; validated in CI only (fmt, validate, tflint), never planned or applied, see docs/context/cloud-mocks.md
# Host and token only from the environment: AWX_HOSTNAME (for example https://awx.example.com) and AWX_TOKEN.
# Nothing here or in tfvars; CI only runs fmt/validate/tflint, never plan/apply.
provider "awx" {}
