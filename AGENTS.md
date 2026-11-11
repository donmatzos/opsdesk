# Agent instructions
Same rules for every coding agent (Claude Code, GitHub Copilot CLI, others): read `CLAUDE.md` first, it is the single entry point and routes to `docs/context/`. Do not duplicate rules here.
- Project skill `.claude/skills/ops-check` (run before committing ops, CI, docs or context changes); agent setup as code in `apm.yml` + `apm.lock.yaml` (`apm install`, dependency pinned to a commit).
