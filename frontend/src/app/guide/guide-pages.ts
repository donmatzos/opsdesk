export interface GuidePage {
  /** Route segment below /guide; '' is the overview at /guide. */
  slug: string;
  /** Markdown file in docs/guide, served as guide/<file>. */
  file: string;
  title: string;
}

export const GUIDE_PAGES: GuidePage[] = [
  { slug: '', file: 'index.md', title: 'Project guide' },
  { slug: 'architecture', file: 'architecture.md', title: 'Architecture' },
  { slug: 'skills', file: 'skills.md', title: 'What this shows' },
  { slug: 'design', file: 'design.md', title: 'Design and accessibility' },
];

/** Glossary has its own view (filter, anchors) but is also a Markdown file in docs/guide. */
export const GLOSSARY_FILE = 'glossary.md';

/** Maps a guide Markdown file name to its route below /guide ('' = overview), or undefined for other files. */
export function slugForFile(file: string): string | undefined {
  if (file === GLOSSARY_FILE) {
    return 'glossary';
  }
  return GUIDE_PAGES.find((p) => p.file === file)?.slug;
}
