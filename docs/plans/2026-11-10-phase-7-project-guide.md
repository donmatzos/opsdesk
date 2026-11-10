# Phase 7: Project Guide + Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The Angular app becomes a usable ops dashboard (services, incidents, ask-the-runbooks) plus a "Project guide" (architecture tour, what this shows per skill, glossary with sources), built from a UI/UX analysis, WCAG 2.2 AA compliant, and published as a static demo with an in-browser API mock on GitHub Pages, so a recruiter can click through without a backend.

**Architecture:** Same Angular 22 workspace (`frontend/`), standalone components, signals, `toSignal`, one feature folder per area (`catalog/`, `incidents/`, `assist/`, `guide/`, `layout/`, `demo/`, `shared/`). Guide content is Markdown in `docs/guide/`; the Angular build copies it as assets (`angular.json` asset glob, input `../docs/guide`, output `guide/`), the app fetches `guide/<file>.md` relative to the base href and renders it with `marked`; relative repo links are rewritten to GitHub URLs, so the same files read correctly on GitHub (README links to them) and in the app. Demo mode is a build configuration `demo` that swaps `src/environments/environment.ts` for `environment.demo.ts` (`demo: true`) and thereby registers `demoApiInterceptor`, which answers every `/api/*` request from in-memory state. `.github/workflows/pages.yml` builds the demo with `--base-href /<repo>/`, copies `index.html` to `404.html` (deep-link fallback), runs an axe/pa11y check against the built site and deploys with `actions/deploy-pages`.

**Tech Stack:** Angular 22.2, Vitest 5 (jsdom, `HttpTestingController`), `marked` (Markdown), `@fontsource/ibm-plex-sans` + `@fontsource/jetbrains-mono` (self-hosted fonts, no Google Fonts request), `axe-core` (dev, component checks), `pa11y-ci` (built-site check, pinned via `npx`), GitHub Pages actions. Pin each new package to its latest stable version at task start (exact version in `package-lock.json`); record versions in `frontend.md` (Task 9).

**Spec:** `docs/plans/2026-11-10-roadmap.md` (row 7 + section "Frontend"), `docs/context/frontend.md`, `docs/context/skills-matrix.md`, `docs/adr/0001-initial-decisions.md`.

**UI/UX starting point (from `ui-ux-pro-max`, to be confirmed or replaced by Task 1):** pattern "real-time operations": data-dense but scannable, status colours green/amber/red always paired with icon + text. Palette slate-based: background `#0F172A`, surface `#1E293B`, muted `#334155`, border `#475569`, text `#F8FAFC`, accent `#22C55E`, danger `#EF4444`; light scheme derived from the same tokens (`#FFFFFF`/`#0F172A`/`#E2E8F0`). Typography: IBM Plex Sans for UI and body, JetBrains Mono for ids, code, metrics (tabular figures). Style: flat, minimal, no glow, visible focus rings, 150-250 ms transitions, `prefers-reduced-motion` honoured. Layout: top bar + left sidebar (two groups: Dashboard, Project guide) at >= 1024 px, disclosure menu below; content max width 72rem, prose max 70ch.

## Global Constraints
- **Task 1's `docs/guide/design.md` is binding** for Tasks 2-9: tokens, layout, navigation, component states and a11y rules come from it. A task that needs to deviate first updates `design.md` (same commit) and says why.
- WCAG 2.2 AA is an acceptance criterion: contrast >= 4.5:1 text / 3:1 UI and large text in both colour schemes, keyboard-only operable, visible focus, skip link, landmarks, one `h1` per page, focus moved to `main` on route change, page titles per route, labels on all inputs, status never by colour alone, `aria-live` for async results, no emoji icons (inline SVG, `aria-hidden="true"`).
- No secrets: the demo has no tokens, keys or credentials; mock data is fictional. No external runtime requests except links the user clicks (fonts self-hosted).
- Colours only via CSS custom properties from `src/styles/_tokens.scss`; no raw hex in components.
- All API types and URLs stay in one `*-api.ts` per feature; the `/api/assist` shape lives only in `assist/assist-api.ts`.
- Every new Angular route is also added to `SpaForwardController` and covered by `SpaRoutingTest` (jar deep links).
- Frontend tests: Vitest, `describe`/`it`, `HttpTestingController`; every page component spec also runs `expectNoAxeViolations` (Task 2 helper).
- `./mvnw -B -ntp verify` green before each commit; `python3 tools/context_check.py` passes; branch `feature/project-guide`; one commit per task, explicit paths.
- Fast loop: `cd frontend && npx ng test --watch=false --include 'src/app/<area>/**'`.

## Review Focus
1. Contrast and keyboard: built Pages site passes `tools/pages-a11y.sh` (pa11y-ci, axe runner, WCAG2AA) for every route in both schemes (Task 8).
2. Deep links on Pages (`/<repo>/incidents/2`, `/<repo>/guide/glossary`) load the right view after a refresh (Task 8); in the jar `SpaRoutingTest` covers the new routes (Task 2).
3. Demo mock enforces the real API rules: transitions OPEN->MITIGATED->RESOLVED, OPEN->RESOLVED, 404/409/422 as ProblemDetail (Task 7).
4. Rendered Markdown cannot inject script (Angular sanitizer stays on; no `bypassSecurityTrust*`) and repo links resolve (`tools/guide_check.py`, Task 5).
5. The jar build is unchanged in behaviour: no interceptor without the `demo` configuration (Task 7).

## Prerequisite check (start of Task 1)
- `/api/assist` (phase 6): compare the real request/response with the shape fixed in the phase 6 plan: `AssistRequest { question: string }` (1..500 chars), `AssistResponse { answer: string; sources: { runbook: string; title: string; section: string; path: string; score: number }[]; model: string; mock: boolean }`. Show a "mock answer" badge when `mock` is true. If different, change only `assist/assist-api.ts` and the demo mock's assist handler. If phase 6 is not merged, build against the assumed shape and list it in `status.md`.
- Angular must accept an asset input outside the project root (`../docs/guide`). If `ng build` rejects it, use the fallback: npm `prebuild`/`pretest` script `node scripts/copy-guide.mjs` copying `../docs/guide` to `src/generated/guide/` (git-ignored) and point the asset glob there.

---

### Task 1: UI/UX analysis (owner requirement, drives all views)

**Files:**
- Create: `docs/guide/design.md` (public "Design & accessibility" guide page, <= 150 lines)

- [ ] **Step 1:** Invoke the `ui-ux-pro-max` skill: `--design-system` with query "devops incident status dashboard developer documentation" and `--domain ux` searches for "accessibility navigation focus", "table data dense", "form error". Audit the current UI (`frontend/src/app/`) against its priority 1-9 rules and write the findings.
- [ ] **Step 2:** Write `design.md` with these sections: Audience and goals (recruiter in 2 minutes, operator), Information architecture (route map below), Navigation and layout (breakpoints 375/768/1024/1440, sidebar vs disclosure menu, skip link, breadcrumbs only in `guide/*`), Visual language (token table: name, light value, dark value, measured contrast ratio against its background), Typography scale (12/14/16/18/24/32, line-height 1.5), Components and states (button, link, table, status badge, form field, alert, empty/loading/error states), Accessibility checklist (each WCAG 2.2 AA criterion this app touches, how it is met, how it is tested), Per-view decisions, Anti-patterns. Confirm or replace each item of the plan's "UI/UX starting point" explicitly.
- [ ] **Step 3:** Route map (fixed for later tasks): `services`, `incidents`, `incidents/:id`, `assist`, `guide` (overview), `guide/architecture`, `guide/skills`, `guide/design`, `guide/glossary`, `**` (not found); `''` redirects to `environment.homeRoute` (`guide` in the demo, `services` in the jar).
- [ ] **Step 4: Verify:** every contrast ratio computed with a script (WCAG relative-luminance formula, paste the command in the PR description), all pairs >= 4.5:1 (text) / 3:1 (UI). **Commit** `docs(guide): ui/ux analysis and design system for the dashboard and guide`.

### Task 2: App shell, design tokens, navigation, a11y foundation

**Files:**
- Create: `frontend/src/styles/_tokens.scss` (custom properties for both schemes via `prefers-color-scheme`), `src/styles/_base.scss` (reset, typography, `:focus-visible` ring, `.visually-hidden`, reduced motion); modify `src/styles.scss` (imports + `@fontsource` CSS), `package.json` (fonts, `axe-core`)
- Create: `src/environments/environment.ts` (`{ demo: false, homeRoute: 'services', repoUrl: 'https://github.com/donmatzos/opsdesk', repoRef: 'main' }`)
- Create: `src/app/layout/shell.ts` (header, demo banner slot, sidebar nav groups, `<main id="main" tabindex="-1">`, footer with repo link), `layout/nav-items.ts` (single source for nav labels + routes), `layout/menu-toggle.ts` (disclosure button, `aria-expanded`, `aria-controls`), `layout/focus-on-navigate.ts` (focuses `#main` after `NavigationEnd`, not on first load), `layout/title-strategy.ts` (`OpsdeskTitleStrategy`: "<route title> · opsdesk")
- Create: `src/app/shared/icon.ts` (`<app-icon name>`: inline SVG paths from Lucide, ISC licence noted in a comment), `shared/status-badge.ts` (icon + text per `UP|DEGRADED|DOWN|OPEN|MITIGATED|RESOLVED|SEV1-3`), `shared/not-found-page.ts`
- Create: `src/testing/axe.ts` (`expectNoAxeViolations(el: HTMLElement)`: runs `axe-core`, `color-contrast` disabled because jsdom has no layout; contrast is checked in Task 8)
- Modify: `app.ts`/`app.html`/`app.scss` (render `<app-shell>`), `app.routes.ts` (full route map from Task 1, `title` per route; guide/assist routes point to placeholder components until their task), `app.config.ts` (`TitleStrategy`, `withComponentInputBinding()`)
- Modify: `backend/.../config/SpaForwardController.java` (`/assist`, `/guide`, `/guide/{page}`), `backend/src/test/.../config/SpaRoutingTest.java` (TestNG `@DataProvider` over all forwarded paths)
- Test: `app.spec.ts` (rewrite), `layout/shell.spec.ts`, `layout/focus-on-navigate.spec.ts`, `layout/title-strategy.spec.ts`, `shared/status-badge.spec.ts`

- [ ] **Step 1: Write the failing tests:** nav has groups "Dashboard" (Services, Incidents, Ask the runbooks) and "Project guide" (Overview, Architecture, What this shows, Design & accessibility, Glossary); active link has `aria-current="page"`; first focusable element is a skip link to `#main`; menu toggle flips `aria-expanded`; focus moves to `#main` on the second navigation only; title is "Incidents · opsdesk"; status badge renders text + `aria-hidden` icon; no axe violations on the shell; `SpaRoutingTest` gets 200 + `<app-root` for `/assist`, `/guide`, `/guide/glossary`.
- [ ] **Step 2:** `cd frontend && npx ng test --watch=false`. Expected: FAIL. **Step 3:** implement per `design.md`. **Step 4:** Expected: PASS; `./mvnw -B -ntp verify` green.
- [ ] **Step 5: Commit** `feat(ui): accessible app shell, design tokens and navigation`.

### Task 3: Services overview and incident views

**Files:**
- Modify: `src/app/catalog/services-page.ts` (+ spec): status summary (count per status, as text), table with `<caption>`, `scope="col"`, status badges, loading/empty/error states
- Create: `src/app/incidents/incident-api.ts` (`Incident { id; serviceId; severity: 'SEV1'|'SEV2'|'SEV3'; state: 'OPEN'|'MITIGATED'|'RESOLVED'; summary; openedAt; resolvedAt: string|null }`, `CreateIncident`, `ProblemDetail`; `IncidentApi.list(state?)`, `get(id)`, `open(req)`, `changeState(id, state)`; `allowedTransitions(state)` mirrors `IncidentState.canTransitionTo`)
- Replace: `incidents/incidents-page.ts` (list: state filter as labelled `<select>`, table with service name joined from `CatalogApi`, link per row to `incidents/:id`, "Open incident" form: service select, severity select, summary `maxlength=200`, inline errors, `role="alert"` for ProblemDetail `detail`)
- Create: `incidents/incident-detail-page.ts` (input `id` via component input binding; fields as `<dl>`, buttons only for allowed transitions, result announced in an `aria-live="polite"` region; 404 -> "Incident not found" with link back)
- Test: `incident-api.spec.ts`, `incidents-page.spec.ts`, `incident-detail-page.spec.ts`

- [ ] **Step 1: Write the failing tests:** `list('OPEN')` calls `/api/incidents?state=OPEN`; `allowedTransitions('MITIGATED')` is `['RESOLVED']`, `'RESOLVED'` is `[]`; page shows service names not ids; submitting the form POSTs and the new row appears; a 422 ProblemDetail shows its `detail` in an alert; detail page shows only "Resolve" for MITIGATED and announces the new state; 404 renders the not-found text; axe clean on all three.
- [ ] **Step 2:** run `--include 'src/app/{catalog,incidents}/**'`, Expected: FAIL. **Step 3:** implement. **Step 4:** Expected: PASS; full verify green. **Step 5: Commit** `feat(ui): services overview and incident list, detail and actions`.

### Task 4: Ask the runbooks

**Files:**
- Create: `src/app/assist/assist-api.ts` (only file that knows the `/api/assist` shape, see Prerequisite check; header comment "shape from the phase 6 plan, verify against the merged API"), `assist/assist-page.ts` (labelled `<textarea>`, submit button disabled while pending, answer in `aria-live="polite"` region, sources as a list of links to the runbook on GitHub via `repoUrl`/`repoRef`; 404/503 -> "The runbook assistant is not available in this deployment")
- Test: `assist-api.spec.ts`, `assist-page.spec.ts`

- [ ] **Step 1: Write the failing tests:** POST body `{question}`; answer and two source links render (`href` ends with `blob/main/docs/runbooks/<file>.md`); button disabled while pending; 404 shows the unavailable message; empty question is not sent and shows a field error; axe clean.
- [ ] **Step 2-4:** run, FAIL; implement; PASS, full verify green. **Step 5: Commit** `feat(ui): ask-the-runbooks view with sources`.

### Task 5: Guide pipeline (Markdown from `docs/guide/` into the app)

**Files:**
- Modify: `frontend/angular.json` (build `assets` += `{ "glob": "**/*.{md,svg}", "input": "../docs/guide", "output": "guide" }`), `package.json` (`marked`)
- Create: `src/app/guide/guide-pages.ts` (`GUIDE_PAGES: { slug; file; title }[]`: `overview`/`index.md`, `architecture`, `skills`, `design`; glossary separate), `guide/guide-content.ts` (`GuideContent.load(file)`: `HttpClient` GET `guide/<file>` as text, no leading slash so the base href applies), `guide/markdown.ts` (`renderMarkdown(md, ctx)`: `marked` with a renderer that adds slug ids to headings, rewrites links: `../../<path>` -> `<repoUrl>/blob/<repoRef>/<path>`, `<page>.md[#x]` -> `guide/<slug>[#x]`, images -> `guide/<file>`; no raw HTML passthrough), `guide/guide-page.ts` (route `guide` and `guide/:page`; binds HTML via `[innerHTML]` (sanitized), intercepts clicks on internal links and calls `router.navigateByUrl`; unknown slug -> not-found; prose width 70ch)
- Create: `tools/guide_check.py` (every relative link/image in `docs/guide/*.md` resolves to a repo path; every glossary entry has exactly one `Source: [label](https://...)` line; exit 1 with a list), add a step to the `context-check` job in `.github/workflows/ci.yml`
- Create: `docs/guide/index.md` (placeholder heading + one paragraph, filled in Task 6)
- Test: `markdown.spec.ts`, `guide-content.spec.ts`, `guide-page.spec.ts`

- [ ] **Step 1: Write the failing tests:** heading `## Error budget` gets `id="error-budget"`; `../../deploy/helm/opsdesk/values.yaml` becomes the GitHub URL; `glossary.md#mttr` becomes `guide/glossary#mttr`; `<script>` in Markdown does not reach the DOM; clicking an internal link navigates without a full reload; unknown slug renders not found; axe clean.
- [ ] **Step 2-4:** run, FAIL; implement; PASS. Also: `ng build` output contains `guide/index.md`; `python3 tools/guide_check.py` passes and fails after adding a broken link (revert). Full verify green.
- [ ] **Step 5: Commit** `feat(guide): render docs/guide markdown in the app with repo links`.

### Task 6: Guide content and glossary view

**Files:**
- Write: `docs/guide/index.md` (what opsdesk is, how to use the demo, a 2-minute path: Services -> incident -> architecture -> skills), `architecture.md` (tour in 6-8 steps: browser -> Angular -> REST -> domain -> PostgreSQL; MCP client -> `/mcp`; `/api/assist` -> retriever -> `LlmClient`; build -> CI -> image -> Helm/OpenShift; metrics/logs; each step links into the repo), `architecture.svg` (hand-written SVG with `<title>`/`<desc>`, token-neutral colours readable on light and dark, text alternative as a list below it in the Markdown), `skills.md` (table Skill | What it shows | Where (repo links), one row per A/B skill in `skills-matrix.md`, mocked parts marked "mocked"), `glossary.md` (one `## Term` per entry: 1-3 sentence definition, `Source: [label](https://...)` from official docs or the primary paper, optional `In this repo: [path](../../...)`; at least MTTR, SLO, SLI, Error budget, Incident severity, Runbook, Postmortem, RAG, MCP, LLM, Helm, Helm chart, Route, Ingress, NetworkPolicy, Liveness/readiness probe, SCC, CI/CD, Reusable workflow, Trivy, gitleaks, SARIF, Testcontainers, Flyway, Micrometer, Prometheus, ECS logging, Datadog, Terraform, Arbitrary UID)
- Create: `src/app/guide/glossary.ts` (`parseGlossary(md): GlossaryEntry[]` with `term, id, definitionHtml, source {label,url}, repoLinks`), `guide/glossary-page.ts` (labelled filter input, result count in `aria-live`, `<dl>` with anchor ids, alphabetical jump list)
- Modify: `README.md` (short "Project guide" section linking `docs/guide/index.md` and the Pages URL `https://donmatzos.github.io/opsdesk/`)
- Test: `glossary.spec.ts`, `glossary-page.spec.ts`

- [ ] **Step 1: Write the failing tests:** parser returns entries with source label + URL, throws on an entry without `Source:`; filter "slo" (case-insensitive, term and definition) shows SLO and hides Helm, the result count is announced; `#mttr` anchor exists; axe clean.
- [ ] **Step 2-4:** run, FAIL; implement; PASS. Check every source URL once with `curl -sIL -o /dev/null -w '%{http_code}'` (200 expected), `python3 tools/guide_check.py` passes, `design.md` contrast rules hold for the SVG. Full verify green.
- [ ] **Step 5: Commit** `feat(guide): architecture tour, skills page and glossary with sources`.

### Task 7: Demo mode with an in-memory API mock

**Files:**
- Create: `src/environments/environment.demo.ts` (`demo: true`, `homeRoute: 'guide'`), `src/app/demo/demo-data.ts` (4 services as in `R__demo_data.sql`, 5 incidents across all states and severities, canned assist answers keyed by keyword with 1-2 sources from `docs/runbooks/`; all fictional), `demo/demo-backend.ts` (state + rules: list/filter, get, open with validation -> 422, unknown service -> 404/422 as the real API, transitions -> 409 `IllegalStateTransition`, `resolvedAt` set), `demo/demo-api.interceptor.ts` (`HttpInterceptorFn`: `/api/*` -> `DemoBackend`, 150-300 ms delay, ProblemDetail errors as `HttpErrorResponse` with `application/problem+json`)
- Modify: `app.config.ts` (`provideHttpClient(withFetch(), withInterceptors(environment.demo ? [demoApiInterceptor] : []))`), `layout/shell.ts` (banner when `environment.demo`: "Demo: runs in your browser with simulated data. Changes reset on reload."), `angular.json` (configuration `demo`: extends production options, `fileReplacements` environment, `outputPath.base: "dist/demo"`), `package.json` (`"build:demo": "ng build --configuration demo"`)
- Test: `demo-backend.spec.ts`, `demo-api.interceptor.spec.ts`

- [ ] **Step 1: Write the failing tests:** GET `/api/services` returns 4; OPEN->MITIGATED->RESOLVED works, RESOLVED->OPEN gives 409 ProblemDetail, unknown id 404, blank summary 422; assist returns answer + sources; non-`/api` requests (`guide/index.md`) pass through untouched; with `environment.demo === false` no interceptor is registered (Review Focus 5).
- [ ] **Step 2-4:** run, FAIL; implement; PASS. `npm run build:demo` succeeds; `npx http-server dist/demo -p 4300` (pinned version) shows the whole app working offline (DevTools: no request to `/api` leaves the browser). Full verify green.
- [ ] **Step 5: Commit** `feat(demo): in-browser api mock and demo build configuration`.

### Task 8: GitHub Pages workflow and built-site accessibility check

**Files:**
- Create: `tools/pages-a11y.sh <site-dir> <base-path>` (copies the site to a temp root under `<base-path>`, serves it with pinned `http-server`, runs pinned `pa11y-ci` with `frontend/.pa11yci.json`: runner axe, standard WCAG2AA, every route of the route map incl. `incidents/2` and `guide/glossary`, once with `prefers-color-scheme: dark` emulated and once light; exit non-zero on any issue)
- Create: `frontend/.pa11yci.json`, `.github/workflows/pages.yml` (on push to `main` and PRs touching `frontend/**`, `docs/guide/**`, the workflow or the script, plus `workflow_dispatch`; job `build` (`contents: read`): checkout, `actions/setup-node` with Node `24.22.2`, `npm ci`, `npm run build:demo -- --base-href "/${{ github.event.repository.name }}/"`, `cp index.html 404.html`, `tools/pages-a11y.sh`, `actions/upload-pages-artifact`; job `deploy` only on `main` and when repo variable `PAGES_DEPLOY == 'true'`: `pages: write`, `id-token: write`, environment `github-pages`, `actions/deploy-pages`; all actions pinned by full SHA with version comment, concurrency group `pages`, no secrets used)
- Modify: `.github/dependabot.yml` only if the new actions are not already covered by the `github-actions` entry

- [ ] **Step 1 (red first):** run `tools/pages-a11y.sh` against the Task 7 build with one token deliberately below 4.5:1: Expected: FAIL naming the contrast issue. Restore: Expected: PASS for all routes and both schemes.
- [ ] **Step 2:** deep links locally: request `/opsdesk/guide/glossary` from the served temp root returns `404.html` and the app renders the glossary (Review Focus 2).
- [ ] **Step 3:** push; PR run of `pages.yml` builds and checks green; deploy skipped (variable unset). `actionlint` (if available) clean. **Commit** `ci(pages): static demo build, a11y check and github pages deploy`.

### Task 9: Docs and status

**Files:**
- Modify: `docs/context/frontend.md` (rewrite: structure per folder, routes + SpaForward rule, tokens/design.md rule, guide pipeline and link rewriting, demo mode + Pages, a11y checks, package versions), `docs/context/status.md` (phase 7 state, next step; open item if `/api/assist` shape is still assumed), `docs/context/testing.md` (axe helper, `tools/pages-a11y.sh`, `tools/guide_check.py`), `docs/context/ops.md` (`pages.yml`, `PAGES_DEPLOY`), `docs/context/project-structure.md` (`docs/guide/`, new tools), `docs/context/skills-matrix.md` (Angular row: done), `docs/plans/2026-11-10-roadmap.md` (row 7 note), `CLAUDE.md` (commands `npm run build:demo`, `tools/pages-a11y.sh`)

- [ ] **Step 1:** `python3 tools/context_check.py` and `python3 tools/guide_check.py` pass; gitleaks on the working tree finds only `deploy/compose/.env`; `./mvnw -B -ntp verify` green.
- [ ] **Step 2:** owner setup noted in `status.md`: Settings -> Pages -> Source "GitHub Actions", repo variable `PAGES_DEPLOY=true`; after merge the Pages URL is clickable without a backend (phase done-criterion).
- [ ] **Step 3: Commit** `docs: phase 7 frontend, guide and pages context`; push; CI and `pages.yml` green.

## Open questions for the owner
1. **Landing route:** demo opens on the guide overview, the jar on Services (`environment.homeRoute`). Default: as planned.
2. **Colour scheme:** follow the OS (light + dark, both AA) without a toggle, vs. dark only (the skill's suggestion). Default: follow the OS.
3. **Fonts:** self-hosted IBM Plex Sans + JetBrains Mono (about 150 kB, no third-party request) vs. system font stack. Default: self-hosted.
4. **Design notes public:** `docs/guide/design.md` shown as a guide page to recruiters. Default: yes, it shows UX and a11y work.
5. **Pages deploy:** gated by repo variable `PAGES_DEPLOY=true` (like `PUBLISH_IMAGE`), Pages source "GitHub Actions". Default: gate on, owner flips it after the first merge.
6. **Phase 6 not merged yet:** build the assist view against the assumed shape and the mock. Default: yes, recheck when phase 6 lands.
