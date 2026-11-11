# Secret exposure

Use this when a credential may have become public or visible to the wrong people: a token or password committed to git, printed in a log, pasted in a chat, or reported by gitleaks or GitHub secret scanning.

## Symptoms
- gitleaks (pre-commit hook or the CI `secrets` job) or GitHub secret scanning reports a finding.
- A credential shows up in a log line, a CI output, a screenshot or a pull request.
- A person tells you they saw a secret where it should not be.

## First checks
- What is it, where does it work, and who could have seen it? A repository that is public means everyone could have seen it from the first push.
- Is it still valid? Assume yes until it is revoked.
- Is it in git history, in a build log, or both? `gitleaks git . --redact` scans the full history without printing the value.

## Mitigation
- Revoke or rotate first, before any clean-up. A removed commit does not make a leaked credential safe. For the MCP bearer token follow `mcp-token-rotation`; for a database password rotate it in the Secret and restart the pods (`pod-not-ready` if they do not come back).
- Replace the value everywhere it is used: Kubernetes Secrets, GitHub Actions secrets, GitLab CI/CD variables, Jenkins credentials, AWX credentials.
- Remove it from git history only after rotating, with `git filter-repo` on a fresh clone, and force-push as a deliberate, announced step; rewriting a shared branch invalidates everyone's clones.
- Run `gitleaks git . --redact` again and check the CI `secrets` job is green.
- Never repeat the value in tickets, chats, pull requests or postmortems; refer to it by name and location.

## Escalation
- The credential protected customer data or a production system, or you cannot tell who used it: open a SEV1 incident (`sev1-incident`) and inform the security contact.

## Related
- `mcp-token-rotation`, `sev1-incident`, `postmortem-template`
- Example: `docs/postmortems/2026-11-10-local-db-password-in-history.md`
