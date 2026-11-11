# Postmortem: local database password committed to git history

> Blameless. No credential value appears here; the password is referred to by name and location only.

## Summary
During phase 1, a local development database password was written into repository files (compose file, configuration defaults and plan text) and committed. It was found during the owner's review on 2026-11-10 and removed from the branch history the same day by a history rewrite; the rule "no secrets in the repository, not even local ones" and two guards were added.

## Impact
A local development password was readable in the public repository's history for a short time. It protected a throwaway local PostgreSQL container, not a shared or production system. Who may have cloned the repository in that window was not measured.

## Timeline (UTC)
| Time | Event |
|---|---|
| 2026-11-10 (time not recorded) | Phase 1 commits contain the local password in several files |
| 2026-11-10 (time not recorded) | Owner review finds it |
| 2026-11-10 (time not recorded) | Branch history rewritten, force-pushed; `main` keeps no value in files |
| 2026-11-10 (time not recorded) | Rule added to `CLAUDE.md`, template `deploy/compose/.env.example`, gitleaks pre-commit hook and CI scan |

## Detection
A person found it in review. No automated check existed yet: gitleaks only arrived with the guards below.

## Root cause
1. A convenient local default was written into files so the project started without setup.
2. The assumption "a local password is not a secret" went unchallenged.
3. There was no automated secret scan in the pre-commit hook or the pipeline at that point.
4. The project rules did not say that local passwords count too.

## What went well
The finding was acted on the same day, and the fix removed the value rather than only the files. The new rule is simple enough to check automatically.

## What went badly
The value reached git history before any guard looked at it. Rewriting history is disruptive and only a safe step because nobody else had work on the branch.

## Where we got lucky
The password only protected a local container.

## Action items
| Action | Owner | Due | Done |
|---|---|---|---|
| Rule: no secrets in the repo, not even local; passwords from env (`CLAUDE.md`) | owner | 2026-11-10 | yes |
| Template `deploy/compose/.env.example` with names only; `.env` git-ignored | owner | 2026-11-10 | yes |
| gitleaks pre-commit hook (`.pre-commit-config.yaml`) | owner | 2026-11-10 | yes |
| gitleaks full-history scan in CI (job `secrets`) | owner | 2026-11-10 | yes |
| Runbook for a credential exposure (`docs/runbooks/secret-exposure.md`) | owner | 2026-11-11 | yes |

## Related
`docs/runbooks/secret-exposure.md`, `docs/context/workflow.md` (secrets section)
