# opsdesk

[![ci](https://github.com/donmatzos/opsdesk/actions/workflows/ci.yml/badge.svg)](https://github.com/donmatzos/opsdesk/actions/workflows/ci.yml) [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=donmatzos_opsdesk&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=donmatzos_opsdesk) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=donmatzos_opsdesk&metric=coverage)](https://sonarcloud.io/summary/new_code?id=donmatzos_opsdesk)

> Work in progress. Skeleton only; see `docs/context/status.md`.

A minimal service-status and incident desk: Spring Boot (Java 25) backend, Angular frontend, an MCP server for AI assistants, shipped as one jar and operated like a production service (CI/CD, container, Helm, Terraform, observability, security scans).

Cloud parts (Azure OpenAI / AI Search, OpenShift, Datadog, Artifactory) are mocked or disabled, because the project runs without a cloud account. They are written the way they would be used, and each one is marked.

<!-- TODO: architecture diagram, quick start, "what this shows" table (from docs/context/skills-matrix.md) -->
