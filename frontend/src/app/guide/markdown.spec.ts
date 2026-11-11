import { extractToc, renderMarkdown, slugify } from './markdown';

const ctx = { repoUrl: 'https://github.com/donmatzos/opsdesk', repoRef: 'main' };

describe('renderMarkdown', () => {
  it('gives headings slug ids', () => {
    expect(renderMarkdown('## Error budget', ctx)).toContain('<h2 id="error-budget">Error budget</h2>');
    expect(slugify('Liveness/readiness probe')).toBe('liveness-readiness-probe');
  });

  it('rewrites repo links to GitHub and opens them in a new tab', () => {
    const html = renderMarkdown('[values](../../deploy/helm/opsdesk/values.yaml)', ctx);
    expect(html).toContain('href="https://github.com/donmatzos/opsdesk/blob/main/deploy/helm/opsdesk/values.yaml"');
    expect(html).toContain('target="_blank"');
    expect(html).toContain('rel="noopener noreferrer"');
  });

  it('rewrites links between guide files to guide routes', () => {
    expect(renderMarkdown('[term](glossary.md#mttr)', ctx)).toContain('href="guide/glossary#mttr"');
    expect(renderMarkdown('[arch](architecture.md)', ctx)).toContain('href="guide/architecture"');
    expect(renderMarkdown('[home](index.md)', ctx)).toContain('href="guide"');
    expect(renderMarkdown('[arch](architecture.md)', ctx)).toContain('class="internal-link"');
  });

  it('keeps external links and marks in-page anchors', () => {
    expect(renderMarkdown('[x](https://example.org/a)', ctx)).toContain('href="https://example.org/a"');
    expect(renderMarkdown('[x](#top)', ctx)).toContain('class="anchor-link"');
  });

  it('serves images from guide/', () => {
    expect(renderMarkdown('![Overview](architecture.svg)', ctx)).toContain('<img src="guide/architecture.svg" alt="Overview">');
  });

  it('drops raw HTML', () => {
    const html = renderMarkdown('before <script>alert(1)</script> after\n\n<div onclick="x()">hi</div>', ctx);
    expect(html).not.toContain('<script');
    expect(html).not.toContain('onclick');
  });

  it('wraps tables in a focusable scroll region', () => {
    const html = renderMarkdown('| a | b |\n|---|---|\n| 1 | 2 |', ctx);
    expect(html).toContain('<section class="table-scroll" tabindex="0" aria-label="Table"');
    expect(html).toContain('</table></section>');
  });
});

describe('extractToc', () => {
  it('lists h2 and h3 headings with the ids the renderer gives them', () => {
    const md = '# Title\n\n## First step\n\ntext\n\n### Detail `code`\n\n## Second\n';
    expect(extractToc(md)).toEqual([
      { id: 'first-step', text: 'First step', level: 2 },
      { id: 'detail-code', text: 'Detail code', level: 3 },
      { id: 'second', text: 'Second', level: 2 },
    ]);
  });

  it('ignores headings inside code fences', () => {
    expect(extractToc('## Real\n\n```\n## not a heading\n```\n')).toEqual([{ id: 'real', text: 'Real', level: 2 }]);
  });
});
