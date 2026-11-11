# Open questions
Answered 2026-11-10 (MCP in jar, PostgreSQL, OpenShift `oc` deploy in CI, SonarCloud, versions). Elastic 2026-11-10: local stack as compose profile (phase 4). Frontend 2026-11-10: dashboard + project guide/glossary + GitHub Pages demo (roadmap phase 7): see `docs/adr/0001-initial-decisions.md`.

Still open (decide before the phase that needs it):
- ~~Local LLM (phase 6)~~ decided 2026-11-11 (plan default): mock only; an Ollama adapter would be one class + profile behind `LlmClient`.
- **Closing/throttling `/api/assist`:** open and unthrottled on purpose (showcase, mock model). Before it is used with profile `azure`: auth or no external route, and/or a rate limit.
- ~~Agent tooling (phase 8)~~ decided 2026-11-11 (plan default): `.claude/skills/ops-check` and `apm.yml` + lockfile are committed.
- **Name:** `opsdesk` (working name).
