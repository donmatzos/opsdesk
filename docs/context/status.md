# Status (current state and next step only)
Last updated: 2026-11-10.

## Where we are
Phase 1 (`feature/foundation`) implemented: Maven multi-module (`frontend`, `backend`), one `opsdesk.jar` with Angular inside; service catalog + incident lifecycle REST API on PostgreSQL (Flyway, demo data in profile `demo`); TestNG + Mockito + Testcontainers (34 backend tests), Vitest (2 frontend tests). Final review done; review fixes: optimistic locking on incidents, generic 500 ProblemDetail, no credentials anywhere in config (branch history rewritten 2026-11-10 to remove a committed local DB password; `main` still has the old phase-1 plan text naming it). CI: `build.yml` (build + test). Branch pushed, PR #1 open, not merged.

## Next up
1. Owner reviews and merges `feature/foundation`.
2. Phase 2 `feature/ci-container` (roadmap `docs/plans/2026-11-10-roadmap.md`): write its plan first.
