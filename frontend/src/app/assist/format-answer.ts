import { renderMarkdown, LinkContext } from '../guide/markdown';

export interface AnswerItem {
  n: number;
  /** "Runbook title / Section" */
  label: string;
  html: string;
}

export interface FormattedAnswer {
  items: AnswerItem[];
  /** Used when the answer is free text (a real model): rendered as Markdown. */
  html: string;
}

/** "[1] Title / Section: text" -> item, or undefined when the line has another shape. */
function parseItem(line: string, ctx: LinkContext): AnswerItem | undefined {
  if (!line.startsWith('[')) {
    return undefined;
  }
  const close = line.indexOf('] ');
  const n = Number(line.slice(1, close));
  const colon = line.indexOf(': ', close);
  if (close < 0 || !Number.isInteger(n) || colon < 0) {
    return undefined;
  }
  // The mock flattens list items into "- a - b"; show them as separate points instead.
  const text = line.slice(colon + 2).replace(/^- /, '').replaceAll(' - ', ' • ');
  return { n, label: line.slice(close + 2, colon), html: renderMarkdown(text, ctx) };
}

/**
 * Makes an answer readable. The mock answer ("MOCK answer ... [1] Title / Section: text") becomes numbered items without
 * the repeated mock sentence (the page shows one badge instead); any other answer is rendered as Markdown. The HTML is
 * bound with [innerHTML], so Angular's sanitizer still applies.
 */
export function formatAnswer(answer: string, mock: boolean, ctx: LinkContext): FormattedAnswer {
  const lines = answer.split('\n').map((l) => l.trim()).filter(Boolean);
  const items = lines.map((l) => parseItem(l, ctx));
  if (mock && items.some(Boolean)) {
    return { items: items.filter((i): i is AnswerItem => !!i), html: '' };
  }
  return { items: [], html: renderMarkdown(answer, ctx) };
}
