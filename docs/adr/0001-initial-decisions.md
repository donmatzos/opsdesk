# ADR 0001: Initial decisions (2026-11-10)
Status: accepted (owner).

| Topic | Decision | Why |
|---|---|---|
| MCP placement | Inside the app jar, Spring AI MCP server (WebMVC), streamable HTTP at `/mcp` | one jar, shares domain services, no second deployable |
| Persistence | PostgreSQL 18 + Flyway, Spring Data JDBC; Testcontainers in tests | shows DB operations and migrations; JDBC fits records |
| CI deploy | OpenShift via `oc` + Helm in GitHub Actions, gated on secrets (`OPENSHIFT_SERVER`, `OPENSHIFT_TOKEN`); skipped when absent | matches the owner's daily work; no own cluster, so the job is off by default. A free Red Hat Developer Sandbox can make it real |
| Static analysis | SonarCloud (token `SONAR_TOKEN`) | real and free for public repos |
| Versions | Java 25, Spring Boot 4.1.2, Spring AI 2.0.x (check Boot 4.1 compatibility in phase 5), TestNG 7.12.0, Angular 22.2, Node 24.22.2 (via frontend-maven-plugin 2.0.2), Maven 3.9.16 via wrapper | latest GA on 2026-11-10 |
