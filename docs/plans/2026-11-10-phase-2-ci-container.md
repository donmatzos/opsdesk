# Phase 2: CI and Container Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A DevSecOps pipeline on GitHub Actions (coverage, SonarCloud, secret scan, Trivy, reusable workflow) and an OpenShift-ready container image published to GHCR from `main`.

**Architecture:** `ci.yml` orchestrates jobs; the Maven build lives in a reusable workflow `maven-verify.yml` (`workflow_call`, the templating idea from the owner's thesis). A multi-stage `Dockerfile` builds the jar, extracts Spring Boot layers and runs them on a UBI JRE as UID 1001 / group 0. Trivy scans the repo and the image; findings go to GitHub code scanning as SARIF.

**Tech Stack:** GitHub Actions, JaCoCo 0.8.16, Vitest coverage (`@vitest/coverage-v8` 5.1.0), SonarCloud (`sonar-maven-plugin` 5.8.0.7211), gitleaks 8.30.1, Trivy (`aquasecurity/trivy-action` 0.36.0), Docker Buildx (`docker/*` actions: build-push 7.4.0, setup-buildx 4.4.1, login 4.8.0, metadata 6.2.0), `eclipse-temurin:25-jdk-noble` (build) and `eclipse-temurin:25-jre-ubi10-minimal` (runtime), Dependabot, pre-commit.

**Spec:** `docs/plans/2026-11-10-roadmap.md` (row 2), `docs/context/ops.md`, `docs/context/skills-matrix.md` (Ops rows), `docs/adr/0001-initial-decisions.md` (SonarCloud).

## Global Constraints
- No secrets in the repo, ever (CLAUDE.md). Workflows use only `GITHUB_TOKEN` and the optional `SONAR_TOKEN` secret; check `git diff --cached` before each commit.
- Every third-party action is pinned to a full commit SHA with a `# vX.Y.Z` comment (look the SHA up via `https://api.github.com/repos/<owner>/<repo>/git/ref/tags/<tag>`; dereference annotated tags).
- `permissions:` default `contents: read` at workflow level; jobs raise only what they need (`security-events: write` for SARIF, `packages: write` only in the push step's job and only on `main`).
- The pipeline must be green without `SONAR_TOKEN` (forks, Dependabot): Sonar is skipped, not failed.
- Image: runs as UID 1001, GID 0, group-writable app dir; must also start under an arbitrary UID with `--read-only` and a tmpfs `/tmp` (OpenShift restricted SCC).
- Trivy gates: severity `HIGH,CRITICAL`, `ignore-unfixed: true`, exit code 1, for image and filesystem; secret scanning blocks.
- Workflows validated locally with `actionlint` (Docker image `rhysd/actionlint`) before each commit that touches them.
- Branch `feature/ci-container`; one commit per task.

## Review Focus
1. Dependabot and fork PRs have no secrets: the Sonar step must be skipped and the job green (Task 3).
2. A container started with a random UID and a read-only root filesystem must boot and pass `/actuator/health` (Task 2).
3. The image must not contain `.env`, `.git`, `target/` caches or `node_modules` from the build context (Task 2).
4. Pull requests must never push an image; only `push` events on `main` push to GHCR (Task 3).
5. An unfixed CVE must not block, a fixable HIGH/CRITICAL must (Task 3 config; verified by reading the Trivy step output).

## Prerequisites (owner, can happen in parallel; nothing blocks Tasks 1-2)
- SonarCloud: sign in with GitHub, import `donmatzos/opsdesk` into organization `donmatzos`, **turn off Automatic Analysis**, create a token, store it as Actions secret `SONAR_TOKEN`.
- After the first image push: GitHub -> Packages -> `opsdesk` -> visibility public, link to the repo.

---

### Task 1: Coverage and Sonar configuration

**Files:**
- Modify: `pom.xml` (properties `sonar.organization=donmatzos`, `sonar.projectKey=donmatzos_opsdesk`, `sonar.host.url=https://sonarcloud.io`, `skipTests=false`; pluginManagement for `jacoco-maven-plugin` 0.8.16 and `sonar-maven-plugin` 5.8.0.7211)
- Modify: `backend/pom.xml` (JaCoCo `prepare-agent` + `report` in `verify`; it appends to the existing `@{argLine}`)
- Modify: `frontend/pom.xml` (`npm-test` execution: `<skip>${skipTests}</skip>`, arguments `run test:ci`; properties `sonar.sources=src`, `sonar.tests=src`, `sonar.test.inclusions=**/*.spec.ts`, `sonar.exclusions=**/*.spec.ts`, `sonar.javascript.lcov.reportPaths=coverage/opsdesk-ui/lcov.info`)
- Modify: `frontend/package.json` (devDependency `@vitest/coverage-v8` 5.1.0; script `test:ci` = `ng test --watch=false --coverage`), `frontend/angular.json` (test options `coverageReporters: ["lcov", "text-summary"]`)
- Modify: `docs/context/testing.md` (coverage reports and where they land)

**Interfaces:**
- Produces: `backend/target/site/jacoco/jacoco.xml`, `frontend/coverage/opsdesk-ui/lcov.info` after `./mvnw verify`; `./mvnw -DskipTests package` skips backend and frontend tests (used by the Dockerfile).

- [ ] **Step 1:** Run `./mvnw -B -ntp verify` then `ls backend/target/site/jacoco/jacoco.xml frontend/coverage/opsdesk-ui/lcov.info`. Expected: both missing (the "failing test" of a config task).
- [ ] **Step 2:** Apply the POM, package.json and angular.json changes; `npm install` in `frontend/` with the plugin-installed Node (`PATH=$PWD/node:$PATH`) to update the lockfile; approve any new install scripts as in phase 1.
- [ ] **Step 3:** Run the Step 1 commands. Expected: build green, both files exist, Vitest prints a coverage summary.
- [ ] **Step 4:** Run `./mvnw -B -ntp -DskipTests package`. Expected: no `Tests run` lines and no Vitest run; `backend/target/opsdesk.jar` exists.
- [ ] **Step 5: Commit** `build: add jacoco and vitest coverage with sonarcloud settings`.

### Task 2: OpenShift-ready container image

**Files:**
- Create: `Dockerfile`, `.dockerignore`
- Modify: `deploy/compose/compose.yml` (service `app`, `profiles: [app]`, `build: ../..`, `depends_on: postgres (service_healthy)`, env `SPRING_PROFILES_ACTIVE=demo`, `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/opsdesk`, `SPRING_DATASOURCE_PASSWORD` from the same `.env`, port 8080)
- Create: `tools/smoke-test.sh` (Bash: `smoke-test.sh <base-url>`; waits up to 60 s for `/actuator/health` = `UP`, then checks `GET /api/services` is 200 JSON and `GET /` contains `<app-root`; exits non-zero with a message on the first failure)
- Modify: `docs/context/ops.md` (Container section: real), `CLAUDE.md` (command `docker compose -f deploy/compose/compose.yml --profile app up --build`)

**Interfaces:**
- Produces: image entrypoint `java -XX:MaxRAMPercentage=75 -jar /app/application.jar` from extracted layers (`java -Djarmode=tools -jar opsdesk.jar extract --layers --launcher --destination /extracted`), port 8080, `USER 1001`, workdir `/app` owned `1001:0` with `g=u`.
- Produces: `tools/smoke-test.sh <base-url>` used by Task 3 and phase 3.

Dockerfile stages: `build` (`eclipse-temurin:25-jdk-noble`; copy sources; `RUN --mount=type=cache,target=/root/.m2 --mount=type=cache,target=/root/.npm ./mvnw -B -ntp -DskipTests package`), `extract` (jarmode tools), `runtime` (`eclipse-temurin:25-jre-ubi10-minimal`; copy the four layer dirs in order dependencies, spring-boot-loader, snapshot-dependencies, application; `HEALTHCHECK` with `curl -fs localhost:8080/actuator/health` if `curl` exists in the base image, otherwise no HEALTHCHECK and a comment that Kubernetes probes cover it).

- [ ] **Step 1: Write the failing check:** `tools/smoke-test.sh` first, then run `tools/smoke-test.sh http://localhost:8080` with nothing running. Expected: exits 1 with "health not UP after 60 s".
- [ ] **Step 2:** Write `.dockerignore` (`.git`, `**/target`, `**/node_modules`, `frontend/node`, `frontend/.angular`, `frontend/coverage`, `**/.env`, `.superpowers`, `.idea`, `.vscode`) and the `Dockerfile`.
- [ ] **Step 3:** `docker build -t opsdesk:dev .` Expected: success; then `docker run --rm --entrypoint sh opsdesk:dev -c 'id -u; find / -name .env -o -name node_modules -o -name .git 2>/dev/null | head'` prints `1001` and nothing else.
- [ ] **Step 4:** `docker compose -f deploy/compose/compose.yml --profile app up -d --build --wait`, then `tools/smoke-test.sh http://localhost:8080`. Expected: exit 0.
- [ ] **Step 5 (Review Focus 2):** run the image a second time with `--user 123456:0 --read-only --tmpfs /tmp` on the compose network against the same Postgres (`-p 8081:8080`), then `tools/smoke-test.sh http://localhost:8081`. Expected: exit 0. If Tomcat needs a writable dir, set `server.tomcat.basedir=/tmp/tomcat` via env in the image, not in `application.yml`.
- [ ] **Step 6:** `docker compose ... --profile app down`. **Commit** `feat(container): add openshift-ready multi-stage image and smoke test`.

### Task 3: DevSecOps pipeline with a reusable Maven workflow

**Files:**
- Create: `.github/workflows/maven-verify.yml` (`on: workflow_call`, inputs `java-version` (string, default `"25"`), `sonar` (boolean, default `false`); secrets `SONAR_TOKEN` (required: false). Steps: checkout (`fetch-depth: 0` for Sonar blame), setup-java with Maven cache, cache `frontend/node` + `~/.npm`, `./mvnw -B -ntp verify`, then `./mvnw -B -ntp sonar:sonar` only `if: inputs.sonar && env.SONAR_TOKEN != ''` (map the secret to `env.SONAR_TOKEN` at job level), upload test + coverage reports on failure)
- Create: `.github/workflows/ci.yml` replacing `build.yml` (delete it). Triggers: `push` (all branches), `pull_request` to `main`. Jobs:
  - `verify`: `uses: ./.github/workflows/maven-verify.yml` with `sonar: true`, `secrets: inherit`.
  - `context-check`: `python3 tools/context_check.py`.
  - `secrets`: gitleaks via `docker run ghcr.io/gitleaks/gitleaks:v8.30.1 git /repo --redact` on the full history (`fetch-depth: 0`); fails on findings.
  - `trivy-fs`: Trivy `scan-type: fs` (vuln, secret, misconfig) -> SARIF -> `github/codeql-action/upload-sarif` (`security-events: write`), then the same scan as a gate with `format: table`, `exit-code: 1`.
  - `image` (`needs: [verify, trivy-fs]`, `packages: write`, `security-events: write`): setup-buildx, metadata (`ghcr.io/donmatzos/opsdesk`, tags: `sha`, branch, `latest` on default branch), build with `load: true` and GHA cache, Trivy image scan (SARIF upload + gate), **smoke test**: generate a per-run DB password (`openssl rand -hex 16`, `::add-mask::`, `$GITHUB_ENV`; never a literal), `docker network create`, `docker run postgres:18-alpine` with it, run the built image with `--user 123456:0 --read-only --tmpfs /tmp` on that network, `tools/smoke-test.sh http://localhost:8080`; then login + push **only** `if: github.event_name == 'push' && github.ref == 'refs/heads/main'`. (Service containers are not used: they start before steps and cannot take a generated password.)
- Create: `.github/dependabot.yml` (ecosystems `maven` `/`, `npm` `/frontend`, `github-actions` `/`, `docker` `/`; weekly; group minor+patch per ecosystem; labels `dependencies`)
- Create: `.pre-commit-config.yaml` (gitleaks hook `rev: v8.30.1`), section "Secrets" in `docs/context/workflow.md` (install: `pipx install pre-commit && pre-commit install`)
- Modify: `docs/context/ops.md` (CI section lists jobs; owner prerequisites), `docs/context/status.md`, `README.md` (CI badge, Sonar quality gate badge)

**Interfaces:**
- Consumes: Task 1 coverage paths and Sonar properties; Task 2 `Dockerfile` and `tools/smoke-test.sh`.
- Produces: reusable workflow `maven-verify.yml` with inputs `java-version`, `sonar` and optional secret `SONAR_TOKEN` (phase 8 refers to it as the GitHub counterpart of the GitLab template).

- [ ] **Step 1:** Write all workflow files; resolve action SHAs; `docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:latest -color`. Expected: no findings.
- [ ] **Step 2:** `pre-commit run --all-files` (or the gitleaks Docker command used in CI). Expected: no leaks.
- [ ] **Step 3:** Commit `ci: add devsecops pipeline with reusable maven workflow`, push `feature/ci-container`.
- [ ] **Step 4 (Review Focus 1, 4):** watch the run (GitHub API `commits/<sha>/check-runs`). Expected on the branch push: `verify`, `context-check`, `secrets`, `trivy-fs`, `image` (incl. smoke test) green; Sonar step skipped if `SONAR_TOKEN` is not set yet; push step skipped (not `main`). Read the Trivy gate outputs (Review Focus 5); a fixable HIGH/CRITICAL is fixed (base image tag, dependency bump), not ignored.
- [ ] **Step 5:** Fix anything red with a follow-up commit; update `status.md` (phase 2 done, next phase 3) and `ops.md`; run `python3 tools/context_check.py`; commit `docs: record phase 2 pipeline`, push.
