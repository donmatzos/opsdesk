# Design and accessibility

How the opsdesk UI is designed and why. Binding for the dashboard and the guide: tokens, layout, navigation and accessibility rules below are what the code follows. Findings come from the `ui-ux-pro-max` analysis (design system, UX and accessibility rules) plus an audit of the first skeleton UI.

## Audience and goals
- **Recruiter, 2 minutes:** understands what the project shows without a backend (the demo runs in the browser), finds architecture, skills and glossary quickly.
- **Operator:** sees which service is unhealthy, opens and changes an incident, asks the runbooks. Dense but scannable, nothing hidden behind hover.

## Audit of the skeleton UI (before this phase)
| Finding | Rule | Fix |
|---|---|---|
| Plain links, no landmarks beyond `header`/`main`, no skip link | Accessibility: skip links, landmarks | App shell with skip link, `nav` groups, `main#main` |
| Incident view was a placeholder | Forms and feedback: empty/loading/error states | Real views with all states |
| No page titles per route, no focus handling on navigation | Navigation: focus on route change | Title strategy, focus moved to `main` |
| Colours as raw values in component styles | Typography and colour: semantic tokens | CSS custom properties only |
| Status as text only | Colour not the only signal | Badge = icon + text + colour |

## Confirmed or changed from the starting point
| Item | Decision |
|---|---|
| Pattern "real-time operations", data-dense but scannable | Confirmed |
| Slate palette, green accent, red danger | Confirmed; dark `border` changed from `#475569` to `#64748B` (the old value is about 2:1 on the background and fails 3:1 for UI parts) |
| Skill suggests dark only | Changed: follows the OS (`prefers-color-scheme`), light and dark designed together, both AA, no toggle |
| IBM Plex Sans + JetBrains Mono | Confirmed, self-hosted (no request to a font CDN) |
| Flat, no glow, 150-250 ms transitions | Confirmed; `prefers-reduced-motion` removes transitions |

## Information architecture (routes)
`services`, `incidents`, `incidents/:id`, `assist`, `guide` (overview), `guide/architecture`, `guide/skills`, `guide/design` (this page), `guide/glossary`, `**` (not found). The empty path redirects to the home route: the guide in the static demo, `services` in the jar.

## Navigation and layout
- Top bar (brand, repo link) and a sidebar with two groups: **Dashboard** (Services, Incidents, Ask the runbooks) and **Project guide** (Overview, Architecture, What this shows, Design and accessibility, Glossary). Same placement on every page.
- Breakpoints 375 / 768 / 1024 / 1440. From 1024 px the sidebar is always visible; below it collapses into a disclosure button (`aria-expanded`, `aria-controls`), no hover-only menus.
- Content max width 72rem, prose max 70ch. Tables scroll horizontally inside their own region on small screens, never the page.
- Skip link first in tab order. One `h1` per page. Breadcrumbs only below `guide/`. After a route change focus moves to `main`; the page title is "page - opsdesk".
- Primary action per view is a single filled button; everything else is secondary.

## Visual language
Colours exist only as CSS custom properties (`src/styles/_tokens.scss`), switched by `prefers-color-scheme`. Surfaces: `bg` (page), `surface` (cards, sidebar, tables). Ratios below are WCAG relative-luminance contrast, computed for every pair.

| Token (on background) | Dark | Ratio | Light | Ratio | Needs |
|---|---|---|---|---|---|
| `text` on `bg` | `#F8FAFC` on `#0F172A` | 17.06 | `#0F172A` on `#FFFFFF` | 17.85 | 4.5:1 |
| `muted` on `bg` | `#CBD5E1` on `#0F172A` | 12.02 | `#475569` on `#FFFFFF` | 7.58 | 4.5:1 |
| `muted` on `surface` | `#CBD5E1` on `#1E293B` | 9.85 | `#475569` on `#F1F5F9` | 6.92 | 4.5:1 |
| `link` on `bg` | `#7DD3FC` on `#0F172A` | 10.71 | `#0369A1` on `#FFFFFF` | 5.93 | 4.5:1 |
| `accent` on `bg` | `#22C55E` on `#0F172A` | 7.83 | `#15803D` on `#FFFFFF` | 5.02 | 4.5:1 |
| `danger` on `bg` | `#F87171` on `#0F172A` | 6.45 | `#B91C1C` on `#FFFFFF` | 6.47 | 4.5:1 |
| `warning` on `bg` | `#FBBF24` on `#0F172A` | 10.69 | `#92400E` on `#FFFFFF` | 7.09 | 4.5:1 |
| `info` on `bg` | `#60A5FA` on `#0F172A` | 7.02 | `#1D4ED8` on `#FFFFFF` | 6.70 | 4.5:1 |
| `on-accent` on `accent` | `#052E16` on `#22C55E` | 6.54 | `#FFFFFF` on `#15803D` | 5.02 | 4.5:1 |
| `border` on `bg` | `#64748B` on `#0F172A` | 3.75 | `#64748B` on `#FFFFFF` | 4.76 | 3:1 (UI) |
| `border` on `surface` | `#64748B` on `#1E293B` | 3.07 | `#64748B` on `#F1F5F9` | 4.34 | 3:1 (UI) |
| `focus` on `bg` | `#93C5FD` on `#0F172A` | 9.90 | `#1D4ED8` on `#FFFFFF` | 6.70 | 3:1 (UI) |

Status colours are never the only signal: every status shows an icon and its name (`UP`, `DEGRADED`, `DOWN`, `OPEN`, `MITIGATED`, `RESOLVED`, `SEV1`-`SEV3`).

## Typography
IBM Plex Sans for text; JetBrains Mono for ids, code and numbers in tables (tabular figures). Scale 12 / 14 / 16 / 18 / 24 / 32 px, body 16 px, line height 1.5, weights 400 / 500 / 600. Text never below 12 px; wrapping preferred over truncation.

## Components and states
- **Button / link:** visible `:focus-visible` ring (2 px, `focus` token, offset 2 px), hover and pressed states distinct, minimum target 44 x 44 px, disabled = reduced emphasis plus the `disabled` attribute.
- **Table:** `caption`, `scope="col"`, row links, numbers in mono, own horizontal scroll region with a focusable wrapper.
- **Form field:** visible `label`, helper text, error below the field with `role="alert"`, first invalid field focused after a failed submit, `maxlength` where the API limits it.
- **Alert / result:** async results in an `aria-live="polite"` region; errors in `role="alert"` with a recovery hint.
- **Empty / loading / error:** every list and detail view has all three, text first (no spinner-only states).
- **Icons:** inline SVG (Lucide, ISC licence), `aria-hidden="true"`, one stroke width; never emoji.

## Accessibility checklist (WCAG 2.2 AA, what this app touches)
| Criterion | How it is met | How it is tested |
|---|---|---|
| 1.1.1 Non-text content | decorative icons hidden, diagram has a text alternative | axe in component specs |
| 1.3.1 Info and relationships | landmarks, headings, table markup, `dl` for details | axe, specs |
| 1.4.1 Use of colour | icon + text for every status | specs |
| 1.4.3 / 1.4.11 Contrast | tokens above | `tools/pages-a11y.sh` on the built site, both schemes |
| 1.4.10 Reflow, 1.4.4 Resize | no horizontal page scroll at 320 px, rem units | manual + pa11y viewport |
| 2.1.1 / 2.4.3 Keyboard, focus order | native controls, DOM order = visual order | specs, manual |
| 2.4.1 Bypass blocks | skip link | spec |
| 2.4.2 Page titled | title per route | spec |
| 2.4.7 / 2.4.11 Focus visible and not obscured | ring token, no sticky element covers focus | manual |
| 2.5.8 Target size | 44 px minimum | CSS review |
| 3.3.1 / 3.3.3 Errors | text next to the field with a fix hint | specs |
| 4.1.3 Status messages | `aria-live` / `role="alert"` | specs |

## Per-view decisions
- **Services:** status counts as text first, then the table. **Incidents:** filter by state (labelled select), open-incident form, row link to the detail. **Incident detail:** only the allowed state changes are offered; the new state is announced.
- **Ask the runbooks:** one labelled text area, answer and sources in a live region, a visible "mock answer" badge when the model is mocked.
- **Guide:** Markdown rendered with a 70ch measure; heading anchors; glossary with filter and result count.

## Anti-patterns avoided
Hover-only information, colour-only status, placeholder-only labels, icon-only buttons without a name, removed focus outlines, emoji as icons, raw hex colours in components, spinner-only loading states, third-party font or script requests.
