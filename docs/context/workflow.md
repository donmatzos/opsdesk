# Workflow
- Branches `feature/|fix/|update/<kebab>` from an up-to-date `main`; owner merges. Never commit to `main`.
- Stage explicit paths; commit at meaningful points; push the branch.
- Session end: update `status.md` (state + next step only, no history).
- Plans/specs go to `docs/plans/` (created with the first plan).

## Secrets
- Never commit credentials, not even local ones; they live in `deploy/compose/.env` (git-ignored), GitHub Actions secrets, cluster Secrets.
- Local hook: `pipx install pre-commit && pre-commit install` (gitleaks, `.pre-commit-config.yaml`). CI scans the full history only after a merge to `main`, so the hook is the PR-time guard.
- `gitleaks dir` on the working tree reports `deploy/compose/.env` by design (it holds the real local password, never tracked).

## Before and after a push
- Before pushing: `./mvnw verify` (exit code 0), `python3 tools/context_check.py`, `git diff --cached` for credentials. Re-read `conventions.md` "Sonar-proof code" against your diff.
- After a push/merge, look at SonarCloud: `curl -s "https://sonarcloud.io/api/issues/search?componentKeys=donmatzos_opsdesk&resolved=false&ps=100"` (add `&branch=main` after a merge). Fix real findings; a false positive is marked in the Sonar UI or suppressed in code with a reason, never ignored silently.
- Review your own change before the PR (`/code-review`): auth/cost exposure of new endpoints, partial failures of remote calls, id/key collisions, startup failures caused by optional features.

