# Design and accessibility

How the opsdesk UI is designed and why. Binding for the dashboard and the guide: tokens, layout, navigation and accessibility rules below are what the code follows. The first version (phase 7) was reviewed with screenshots on 2026-11-11 and reworked; the findings are listed here so the reasons stay visible.

## Audience and goals
- **Recruiter, 2 minutes:** understands what the project shows without a backend (the demo runs in the browser), finds architecture, skills and glossary quickly.
- **Operator:** sees what is unhealthy and what needs attention, opens and moves an incident, asks the runbooks. Dense but scannable, nothing behind hover.

## Rework analysis (owner screenshots, 2026-11-11)
| Finding in the first version | Rule | Change |
|---|---|---|
| Sidebar had only a minimum height: its background stopped mid-page and on long pages (glossary) it scrolled away, so navigation was unreachable | Navigation: persistent nav, predictable placement | Sticky header, sticky full-height sidebar with its own scroll; a check scrolls a long page and measures the sidebar |
| Empty top bar, footer bar repeating the demo note, content hugging the left on wide screens | Layout: use the space, one purpose per region | Header with logo, theme toggle, GitHub icon; note moved into the sidebar; wider content, right rail on guide pages |
| No overview: the landing page was a table with a sentence above it | Visual hierarchy, scannability | New Overview: status tiles, needs-attention list, service cards |
| SEV2 and MITIGATED looked identical (amber triangle, plain text); no pill shapes | Colour not the only signal, one meaning per shape | Status = pill with icon and text; severity = text-only square tag |
| Incidents: form first (about 60% of the screen), list below, state as a stacked select | Progressive disclosure, list first | List first with state chips and counts; "Open incident" disclosure panel; whole row is a link target |
| Assist answers showed raw Markdown, repeated the mock sentence, sources below the fold | Content clarity | Example chips, numbered answer items with inline code, mock badge once, source cards |
| Glossary: one long column, "In this repo" on its own line, letter list scrolled away | Findability | Two-column cards, sticky letter rail, source and repo link on one line |
| Phone: status column clipped, wrapped header, menu pushed content down without a scrim | Responsive, no horizontal clipping, modal behaviour | Tables become card lists below 768 px; off-canvas drawer with scrim, Escape, close on navigation, `inert` main |
| Only the OS colour scheme, no way to choose | Theme control | System / Light / Dark toggle, stored, applied before first paint |
| Kept: contrast-checked tokens, status never by colour alone, labelled forms, skip link, focus to main, deep links, self-hosted fonts | | |

## Visual language
Palette **Indigo** (chosen from two contrast-checked options): cool blue-grey layered surfaces (`bg` page, `surface` cards and bars, `surface-2` hover and table heads), indigo accent, soft status tints for badges. Colours exist only as CSS custom properties in `src/styles/_tokens.scss`; dark applies for the OS preference or `data-theme="dark"`. `python3 tools/contrast_check.py` checks every pair of the real token file (also in CI); selected pairs:

| Pair (foreground on background) | Light | Ratio | Dark | Ratio | Needs |
|---|---|---|---|---|---|
| `text` on `bg` | `#111827` on `#f6f7fb` | 16.57 | `#e8eaf2` on `#0b0d14` | 16.16 | 4.5:1 |
| `text` on `surface-2` | `#111827` on `#edeff7` | 15.45 | `#e8eaf2` on `#1c2133` | 13.30 | 4.5:1 |
| `muted` on `surface` | `#4b5563` on `#ffffff` | 7.56 | `#a5acc4` on `#141824` | 7.84 | 4.5:1 |
| `link` on `bg` | `#3730a3` on `#f6f7fb` | 9.28 | `#c7d2fe` on `#0b0d14` | 13.01 | 4.5:1 |
| `accent` on `bg` | `#4338ca` on `#f6f7fb` | 7.38 | `#818cf8` on `#0b0d14` | 6.51 | 4.5:1 |
| `on-accent` on `accent` | `#ffffff` on `#4338ca` | 7.90 | `#0b0d14` on `#818cf8` | 6.51 | 4.5:1 |
| `accent-text` on `accent-soft` | `#3730a3` on `#e0e7ff` | 8.06 | `#c7d2fe` on `#25285a` | 9.20 | 4.5:1 |
| `ok` on `ok-soft` | `#166534` on `#dcfce7` | 6.49 | `#86efac` on `#12331f` | 9.85 | 4.5:1 |
| `warning` on `warning-soft` | `#92400e` on `#fef3c7` | 6.37 | `#fcd34d` on `#3a2a0a` | 9.61 | 4.5:1 |
| `danger` on `danger-soft` | `#b91c1c` on `#fee2e2` | 5.30 | `#fca5a5` on `#3f1414` | 8.37 | 4.5:1 |
| `info` on `info-soft` | `#1d4ed8` on `#dbeafe` | 5.49 | `#93c5fd` on `#172554` | 8.15 | 4.5:1 |
| `strong` on `surface` | `#6b7280` on `#ffffff` | 4.83 | `#8e97b5` on `#141824` | 6.11 | 3:1 |
| `focus` on `bg` | `#4338ca` on `#f6f7fb` | 7.38 | `#a5b4fc` on `#0b0d14` | 9.74 | 3:1 |

Decorative separators and card outlines (`border`) are not required to reach 3:1; the borders of controls (`strong`) are.

## Theme
Segmented control of three native radio buttons (System, Light, Dark) in the header; icons plus text, text hidden on phones but still the accessible name. The choice is stored in `localStorage`; an inline script in `index.html` applies it before first paint, so there is no flash. "System" removes the override and follows `prefers-color-scheme`.

## Information architecture (routes)
`overview` (landing in the app), `services`, `incidents`, `incidents/:id`, `assist`, `guide` (introduction), `guide/architecture`, `guide/skills`, `guide/design` (this page), `guide/glossary`, `**` (not found). The empty path redirects to the home route: the guide in the static demo, the overview in the jar.

## Navigation and layout
- Sticky header (logo, theme toggle, GitHub icon link) and a sticky full-height sidebar with two groups: **Dashboard** (Overview, Services, Incidents, Ask the runbooks) and **Project guide** (Introduction, Architecture, What this shows, Design and accessibility, Glossary). The showcase note sits at the bottom of the sidebar. Same placement on every page.
- From 1024 px the sidebar is always visible. Below it is an off-canvas drawer opened by a labelled menu button (`aria-expanded`, `aria-controls`): scrim, Escape, closes after choosing a link, focus moves in and returns to the button, `<main>` is inert while open.
- Breakpoints 375 / 768 / 1024 / 1440; content max width 76rem, prose 70ch. Guide pages have a sticky "On this page" list from 1200 px. Tables never scroll the page: below 768 px they become card lists.
- Skip link first in tab order, one `h1` per page, focus to `main` and scroll to top after navigation, page title "page · opsdesk".

## Components and states
- **Badges:** status pill (soft tint, icon + text) vs severity tag (square, mono text). **Tile / card:** `surface`, 1px `border`, soft shadow. **Chips:** filter buttons with `aria-pressed` and counts.
- **Button / link:** 2px `focus` ring, 44 px targets, disabled = reduced emphasis plus the attribute. Primary action per view is one filled button.
- **Form field:** visible label, helper text, error below the field with the field marked invalid, first invalid field focused. Optional forms open in a disclosure panel.
- **Results:** async results in `aria-live="polite"`, errors in `role="alert"` with a way to retry; every list has loading, empty and error states (text first).
- **Icons:** inline SVG (Lucide, ISC), `aria-hidden`; never emoji.

## Accessibility checklist (WCAG 2.2 AA, what this app touches)
| Criterion | How it is met | How it is tested |
|---|---|---|
| 1.1.1 Non-text content | decorative icons hidden, diagram has a text alternative | axe in specs |
| 1.3.1 Info and relationships | landmarks, headings, table and `dl` markup | axe, specs |
| 1.4.1 Use of colour | icon + text for every status | specs |
| 1.4.3 / 1.4.11 Contrast | tokens above | `tools/contrast_check.py`; axe on the built site, both schemes |
| 1.4.10 Reflow | no horizontal page scroll at 320 px, card tables | built-site check at phone width |
| 2.1.1 / 2.4.3 Keyboard, focus order | native controls, DOM order = visual order, drawer focus handling | specs, manual |
| 2.4.1 Bypass blocks | skip link | spec |
| 2.4.2 Page titled | title per route | spec |
| 2.4.7 / 2.4.11 Focus visible, not obscured | ring token; sticky header does not cover focus targets | manual |
| 2.5.8 Target size | 44 px minimum | CSS review |
| 3.3.1 / 3.3.3 Errors | text next to the field with a fix hint | specs |
| 4.1.3 Status messages | `aria-live` / `role="alert"` | specs |

## Checks that guard this design
`tools/pages-a11y.sh` runs axe in Chrome on every route (light and dark, desktop and phone, open drawer), forces each scheme through the toggle and checks it survives a reload, and scrolls a long page to assert the sidebar still spans header to window bottom. The contrast check and the sidebar check were each seen failing (low-contrast token, non-sticky sidebar) before they were trusted.

## Anti-patterns avoided
Hover-only information, colour-only status, placeholder-only labels, icon-only buttons without a name, removed focus outlines, emoji as icons, raw hex colours in components, spinner-only loading states, `!important` overrides, click handlers on non-interactive elements, third-party font or script requests.
