# opsdesk project guide

opsdesk is a small **service-status and incident desk**: a Spring Boot backend and an Angular frontend in one jar, an MCP server for AI assistants, and the operations around it that a production service needs. This guide explains what is in the repository and why. The same Markdown files are shown in the app and on GitHub.

The cloud parts (Azure OpenAI and AI Search, OpenShift, Datadog, Artifactory) are **mocked or switched off by default**, because the project runs without a cloud account. Every mock is marked as such in the code and listed in [the cloud mocks table](../context/cloud-mocks.md).

## A two-minute path
1. **[Services](../../frontend/src/app/catalog/services-page.ts)** in the app: which service is healthy, degraded or down.
2. **Incidents:** open one, then move it forward. Incidents only go OPEN, MITIGATED, RESOLVED; a resolved incident is never reopened.
3. **Ask the runbooks:** a question is answered from the operations runbooks, with the sections it used as sources. In this build the answer text comes from a mock model and says so.
4. Read the [architecture tour](architecture.md), then [what this project shows](skills.md).
5. Unknown word? The [glossary](glossary.md) explains each term and links to its source.

## About this demo
- On GitHub Pages the app runs entirely in your browser against simulated data; changes reset when you reload. Nothing is sent to a server.
- In the real build the same app talks to the Spring Boot API on PostgreSQL.
- How the interface was designed, including the accessibility rules it follows: [design and accessibility](design.md).
