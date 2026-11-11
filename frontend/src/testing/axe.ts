import axe from 'axe-core';

/**
 * Fails when axe finds accessibility violations in `el`. `color-contrast` is off because jsdom has no layout; contrast
 * is checked on the built site (tools/pages-a11y.sh). `region` is off for single components (they sit outside the
 * app shell's landmarks); the shell spec turns it on.
 */
export async function expectNoAxeViolations(el: HTMLElement, options: { region?: boolean } = {}): Promise<void> {
  const result = await axe.run(el, {
    rules: { 'color-contrast': { enabled: false }, region: { enabled: options.region ?? false } },
  });
  const summary = result.violations.map((v) => `${v.id}: ${v.help} (${v.nodes.map((n) => n.target.join(' ')).join('; ')})`);
  expect(summary).toEqual([]);
}
