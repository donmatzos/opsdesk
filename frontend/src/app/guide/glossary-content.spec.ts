import { readFileSync } from 'node:fs';
import { parseGlossary } from './glossary';

const REQUIRED = [
  'MTTR', 'SLO', 'SLI', 'Error budget', 'Incident severity', 'Runbook', 'Postmortem', 'RAG', 'MCP', 'LLM', 'Helm',
  'Helm chart', 'Route', 'Ingress', 'NetworkPolicy', 'Liveness/readiness probe', 'SCC', 'CI/CD', 'Reusable workflow',
  'Trivy', 'gitleaks', 'SARIF', 'Testcontainers', 'Flyway', 'Micrometer', 'Prometheus', 'ECS logging', 'Datadog',
  'Terraform', 'Arbitrary UID',
];

describe('docs/guide/glossary.md', () => {
  const entries = parseGlossary(readFileSync('../docs/guide/glossary.md', 'utf-8'), {
    repoUrl: 'https://github.com/donmatzos/opsdesk',
    repoRef: 'main',
  });

  it('contains every required term, each with an https source', () => {
    const terms = entries.map((e) => e.term);
    expect(REQUIRED.filter((t) => !terms.includes(t))).toEqual([]);
    expect(entries.filter((e) => !e.source.url.startsWith('https://'))).toEqual([]);
  });

  it('has unique anchor ids', () => {
    const ids = entries.map((e) => e.id);
    expect(new Set(ids).size).toBe(ids.length);
  });
});
