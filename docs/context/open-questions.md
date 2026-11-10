# Open questions
Answered 2026-11-10 (MCP in jar, PostgreSQL, OpenShift `oc` deploy in CI, SonarCloud, versions): see `docs/adr/0001-initial-decisions.md`.

Still open (decide before the phase that needs it):
- **Frontend scope (phase 7):** dashboard + project guide/glossary + GitHub Pages demo (recommended, see roadmap) or a pure documentation site.
- **Elastic (phase 4):** only ECS JSON logs, or also Elasticsearch + Kibana in compose.
- **Local LLM (phase 6):** mock only, or optional Ollama profile.
- **Agent tooling (phase 8):** `.claude/skills/` and `apm.yml` in the repo, or only `CLAUDE.md`/`AGENTS.md`.
- **Name:** `opsdesk` (working name).
