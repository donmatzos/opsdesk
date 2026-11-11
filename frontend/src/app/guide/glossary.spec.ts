import { filterGlossary, parseGlossary } from './glossary';

const ctx = { repoUrl: 'https://github.com/donmatzos/opsdesk', repoRef: 'main' };
const MD = [
  '# Glossary',
  '',
  'Intro text that is not an entry.',
  '',
  '## MTTR',
  '',
  'Mean time to `resolve`.',
  '',
  'Source: [Atlassian metrics](https://www.atlassian.com/incident-management/kpis/common-metrics)',
  '',
  'In this repo: [IncidentMetrics](../../backend/src/main/java/at/donmatzos/opsdesk/metrics/IncidentMetrics.java)',
  '',
  '## SLO',
  '',
  'A target for an SLI.',
  '',
  'Source: [Google SRE book](https://sre.google/sre-book/service-level-objectives/)',
  '',
  '## Helm',
  '',
  'Package manager for Kubernetes.',
  '',
  'Source: [Helm docs](https://helm.sh/docs/)',
].join('\n');

describe('parseGlossary', () => {
  it('returns entries with id, definition, source and repo link', () => {
    const entries = parseGlossary(MD, ctx);
    expect(entries.map((e) => e.term)).toEqual(['MTTR', 'SLO', 'Helm']);
    const mttr = entries[0];
    expect(mttr.id).toBe('mttr');
    expect(mttr.definition).toBe('Mean time to resolve.');
    expect(mttr.source).toEqual({ label: 'Atlassian metrics', url: 'https://www.atlassian.com/incident-management/kpis/common-metrics' });
    expect(mttr.repoHtml).toContain('https://github.com/donmatzos/opsdesk/blob/main/backend/src/main/java/at/donmatzos/opsdesk/metrics/IncidentMetrics.java');
    expect(entries[1].repoHtml).toBe('');
  });

  it('throws for an entry without a Source line', () => {
    expect(() => parseGlossary('## Orphan\n\nNo source here.\n', ctx)).toThrowError(/Orphan/);
  });

  it('rejects a non-https source', () => {
    expect(() => parseGlossary('## X\n\ntext\n\nSource: [a](http://example.org)\n', ctx)).toThrowError(/X/);
  });
});

describe('filterGlossary', () => {
  it('matches term and definition case-insensitively', () => {
    const entries = parseGlossary(MD, ctx);
    expect(filterGlossary(entries, 'slo').map((e) => e.term)).toEqual(['SLO']);
    expect(filterGlossary(entries, 'KUBERNETES').map((e) => e.term)).toEqual(['Helm']);
    expect(filterGlossary(entries, '  ').length).toBe(3);
  });
});
