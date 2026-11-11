import { renderMarkdown, slugify, LinkContext } from './markdown';

export interface GlossaryEntry {
  term: string;
  /** Anchor id, also the fragment other guide pages link to (`glossary.md#mttr`). */
  id: string;
  /** Plain-text definition (used for filtering). */
  definition: string;
  definitionHtml: string;
  source: { label: string; url: string };
  /** "In this repo" link as HTML, or '' when the term has no counterpart in the repository. */
  repoHtml: string;
}

const SOURCE = /^Source: \[([^\]]+)\]\((https:\/\/[^)\s]+)\)\s*$/;
const REPO = /^In this repo: (.+)$/;

/** Parses docs/guide/glossary.md: one `## Term` per entry with a definition, one `Source:` line and an optional `In this repo:` line. */
export function parseGlossary(markdown: string, ctx: LinkContext): GlossaryEntry[] {
  return markdown
    .split(/^## /m)
    .slice(1)
    .map((block) => {
      const [heading, ...lines] = block.split('\n');
      const term = heading.trim();
      let source: GlossaryEntry['source'] | undefined;
      let repo = '';
      const definition: string[] = [];
      for (const line of lines) {
        const s = SOURCE.exec(line);
        const r = REPO.exec(line);
        if (s) {
          source = { label: s[1], url: s[2] };
        } else if (r) {
          repo = r[1];
        } else {
          definition.push(line);
        }
      }
      if (!source) {
        throw new Error(`Glossary entry "${term}" has no "Source: [label](https://...)" line`);
      }
      const text = definition.join(' ').replace(/\s+/g, ' ').trim();
      return {
        term,
        id: slugify(term),
        definition: text.replace(/[`*_]/g, ''),
        definitionHtml: renderMarkdown(text, ctx),
        source,
        repoHtml: repo ? renderMarkdown(repo, ctx) : '',
      };
    });
}

/** Case-insensitive match on term and definition. */
export function filterGlossary(entries: GlossaryEntry[], query: string): GlossaryEntry[] {
  const q = query.trim().toLowerCase();
  return q ? entries.filter((e) => e.term.toLowerCase().includes(q) || e.definition.toLowerCase().includes(q)) : entries;
}
