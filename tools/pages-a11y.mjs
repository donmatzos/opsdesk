// Accessibility check of the built static site with a real browser: axe-core (WCAG 2.0/2.1/2.2 A and AA, including
// colour contrast) on every route, in light and dark colour scheme, desktop and phone width, plus the open phone menu.
// Every route is opened directly (a deep link), which also proves the 404.html fallback works.
// Usage (via tools/pages-a11y.sh): node pages-a11y.mjs <base-url> <base-path>
import { createRequire } from 'node:module';
import puppeteer from 'puppeteer';

const require = createRequire(import.meta.url);
const [baseUrl, basePath] = process.argv.slice(2);
const ROUTES = ['overview', 'guide', 'guide/architecture', 'guide/skills', 'guide/design', 'guide/glossary', 'services', 'incidents', 'incidents/2', 'assist', 'no-such-page'];
const SCHEMES = ['light', 'dark'];
const VIEWPORTS = [
  { name: 'desktop', width: 1280, height: 900 },
  { name: 'phone', width: 375, height: 800 },
];
const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];

const browser = await puppeteer.launch({
  executablePath: process.env.CHROME_PATH || undefined,
  args: ['--no-sandbox', '--disable-setuid-sandbox'],
});
const failures = [];
let checked = 0;

async function audit(page, label) {
  await page.addScriptTag({ path: require.resolve('axe-core') });
  const result = await page.evaluate(async (tags) => await globalThis.axe.run(document, { runOnly: { type: 'tag', values: tags } }), TAGS);
  checked++;
  for (const v of result.violations) {
    failures.push(`${label}: ${v.id} (${v.impact}) ${v.help}\n    ${v.nodes.slice(0, 3).map((n) => n.target.join(' ') + ' -> ' + (n.any[0]?.message ?? '')).join('\n    ')}`);
  }
}

/** The reported bug: on a long page the sidebar must stay pinned under the header and reach the bottom of the window. */
async function checkSidebarStaysFull(page, label) {
  await page.goto(`${baseUrl}${basePath}guide/glossary`, { waitUntil: 'networkidle0' });
  await page.waitForSelector('main h1');
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
  const box = await page.evaluate(() => {
    const r = document.querySelector('nav.sidebar').getBoundingClientRect();
    return { top: Math.round(r.top), bottom: Math.round(r.bottom), header: Math.round(document.querySelector('header.topbar').getBoundingClientRect().bottom), height: window.innerHeight };
  });
  if (Math.abs(box.top - box.header) > 1 || box.bottom < box.height - 1) {
    failures.push(`sidebar ${label}: after scrolling it spans ${box.top}..${box.bottom}px, expected ${box.header}..${box.height}px`);
  }
}

/** The theme toggle forces a scheme regardless of the OS, stays AA and survives a reload (stored, set before first paint). */
async function checkThemeToggle(page, osScheme) {
  const target = osScheme === 'light' ? 'dark' : 'light';
  await page.goto(`${baseUrl}${basePath}services`, { waitUntil: 'networkidle0' });
  await page.waitForSelector('main h1');
  await page.click(`label:has(input[value="${target}"])`);
  const applied = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
  if (applied !== target) {
    failures.push(`toggle [os ${osScheme}]: choosing ${target} set data-theme="${applied}"`);
  }
  await audit(page, `services via toggle -> ${target} [os ${osScheme}]`);
  await page.reload({ waitUntil: 'networkidle0' });
  const kept = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
  if (kept !== target) {
    failures.push(`toggle [os ${osScheme}]: ${target} was not kept after a reload (got ${kept})`);
  }
  await page.evaluate(() => localStorage.clear());
  await page.reload({ waitUntil: 'networkidle0' });
}

try {
  for (const scheme of SCHEMES) {
    for (const viewport of VIEWPORTS) {
      const page = await browser.newPage();
      await page.setViewport({ width: viewport.width, height: viewport.height });
      await page.emulateMediaFeatures([
        { name: 'prefers-color-scheme', value: scheme },
        { name: 'prefers-reduced-motion', value: 'reduce' },
      ]);
      for (const route of ROUTES) {
        const url = `${baseUrl}${basePath}${route}`;
        const label = `${route} [${scheme}, ${viewport.name}]`;
        await page.goto(url, { waitUntil: 'networkidle0' });
        await page.waitForSelector('main h1', { timeout: 10000 });
        await page.waitForFunction(() => !document.querySelector('main output'), { timeout: 10000 });
        const title = await page.title();
        if (!title.endsWith('opsdesk')) {
          failures.push(`${label}: deep link did not render the app (title "${title}")`);
        }
        const mode = await page.evaluate(() => (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'));
        if (mode !== scheme) {
          failures.push(`${label}: colour scheme emulation failed (got ${mode})`);
        }
        await audit(page, label);
      }
      if (viewport.name === 'desktop') {
        await checkSidebarStaysFull(page, `[${scheme}]`);
        await checkThemeToggle(page, scheme);
      }
      if (viewport.name === 'phone') {
        await page.goto(`${baseUrl}${basePath}services`, { waitUntil: 'networkidle0' });
        await page.waitForSelector('main h1');
        await page.click('button.menu-toggle');
        await audit(page, `services with open menu [${scheme}, phone]`);
      }
      await page.close();
    }
  }
} finally {
  await browser.close();
}

if (failures.length) {
  console.error(`FAIL: ${failures.length} problem(s) in ${checked} checks\n\n${failures.join('\n\n')}`);
  process.exit(1);
}
console.log(`ok: ${checked} checks (${ROUTES.length} routes x ${SCHEMES.length} schemes x ${VIEWPORTS.length} viewports, open phone menu, forced theme via the toggle, sticky sidebar), no WCAG A/AA violations`);
