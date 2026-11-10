# Context index (one line per file; read only what the task needs)
Always read: `CLAUDE.md`, this file, `status.md`. Limits: <= 100 lines, 6 KB per file (`tools/context_check.py`). Runbooks: `docs/runbooks/`. Decisions: `docs/adr/`. Plans: `docs/plans/` (roadmap + one per phase; read only the current phase).

| File | Purpose | Read when |
|---|---|---|
| `status.md` | current state, next step | always (3rd) |
| `skills-matrix.md` | resume skill -> where the project shows it, priority | scoping, README, "is skill X covered?" |
| `open-questions.md` | choices still open, per phase | planning a phase |
| `architecture.md` | modules, domain, data flow, profiles | backend / AI / MCP code |
| `project-structure.md` | directory tree, where new things go | finding your way |
| `conventions.md` | Java, TypeScript, Maven, naming, comments for mocks | before writing code |
| `testing.md` | TestNG, Mockito, frontend tests, what CI runs | before running/adding tests |
| `workflow.md` | branches, commits, session routine | before git work |
| `mcp.md` | MCP server: transport, tools, how to try it | MCP work |
| `frontend.md` | Angular app, build into the jar | frontend work |
| `ops.md` | CI/CD, Docker, Helm/OpenShift, Terraform, observability, security | ops work |
| `cloud-mocks.md` | how mocked cloud parts are written and switched | Azure / OpenShift / Datadog / Artifactory |
