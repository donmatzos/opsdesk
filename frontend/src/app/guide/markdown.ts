import { Marked, Tokens } from 'marked';
import { slugForFile } from './guide-pages';

export interface LinkContext {
  repoUrl: string;
  repoRef: string;
}

/** Lower-case, accents dropped, anything else becomes '-': "Error budget" -> "error-budget". */
export function slugify(text: string): string {
  return text
    .normalize('NFKD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-)|(-$)/g, '');
}

const ABSOLUTE = /^(https?:|mailto:)/i;

/** Where a link in a guide file points to: another guide page, the repo on GitHub, an anchor, or an external site. */
function resolveHref(href: string, ctx: LinkContext): { href: string; kind: 'external' | 'internal' | 'anchor' } {
  if (ABSOLUTE.test(href)) {
    return { href, kind: 'external' };
  }
  if (href.startsWith('#')) {
    return { href, kind: 'anchor' };
  }
  const [path, fragment] = href.split('#');
  if (path.startsWith('../../')) {
    const repoPath = path.slice('../../'.length);
    return { href: `${ctx.repoUrl}/blob/${ctx.repoRef}/${repoPath}${fragment ? '#' + fragment : ''}`, kind: 'external' };
  }
  const slug = slugForFile(path);
  if (slug !== undefined) {
    return { href: `guide${slug ? '/' + slug : ''}${fragment ? '#' + fragment : ''}`, kind: 'internal' };
  }
  return { href: `guide/${path}`, kind: 'internal' };
}

/**
 * Markdown -> HTML for the guide. Raw HTML in the Markdown is dropped here, and the result is bound with [innerHTML],
 * so Angular's sanitizer still runs on top (no bypassSecurityTrust anywhere).
 */
export function renderMarkdown(markdown: string, ctx: LinkContext): string {
  const marked = new Marked({
    renderer: {
      html: () => '',
      heading(this, token: Tokens.Heading): string {
        const inner = this.parser.parseInline(token.tokens);
        return `<h${token.depth} id="${slugify(token.text)}">${inner}</h${token.depth}>\n`;
      },
      link(this, token: Tokens.Link): string {
        const target = resolveHref(token.href, ctx);
        const inner = this.parser.parseInline(token.tokens);
        if (target.kind === 'external') {
          return `<a href="${target.href}" target="_blank" rel="noopener noreferrer">${inner}<span class="visually-hidden"> (opens in a new tab)</span></a>`;
        }
        return `<a href="${target.href}" class="${target.kind === 'anchor' ? 'anchor-link' : 'internal-link'}">${inner}</a>`;
      },
      image(token: Tokens.Image): string {
        const src = resolveHref(token.href, ctx).href;
        return `<img src="${src}" alt="${token.text}">`;
      },
    },
  });
  const html = marked.parse(markdown, { async: false });
  return html
    .replaceAll('<table>', '<section class="table-scroll" tabindex="0" aria-label="Table"><table>')
    .replaceAll('</table>', '</table></section>');
}

export interface TocEntry {
  id: string;
  text: string;
  level: 2 | 3;
}

/** The h2 and h3 headings of a Markdown page, for the "On this page" list (ids match the rendered headings). */
export function extractToc(markdown: string): TocEntry[] {
  const entries: TocEntry[] = [];
  let inFence = false;
  for (const line of markdown.split('\n')) {
    if (line.trimStart().startsWith('```')) {
      inFence = !inFence;
    } else if (!inFence && (line.startsWith('## ') || line.startsWith('### '))) {
      const level = line.startsWith('### ') ? 3 : 2;
      const text = line.slice(level + 1).replace(/[`*_]/g, '').trim();
      entries.push({ id: slugify(text), text, level });
    }
  }
  return entries;
}

