import { formatAnswer } from './format-answer';

const ctx = { repoUrl: 'https://github.com/donmatzos/opsdesk', repoRef: 'main' };
const MOCK = [
  'MOCK answer (no language model was called). The most relevant runbook sections:',
  '[1] Pod not ready / Symptoms: - The readiness probe is failing: `/readyz` does not return 200. - `CrashLoopBackOff` in `oc get pods`.',
  '[2] Pod not ready / Overview: Use this when pods are `0/1 Ready`.',
].join('\n');

describe('formatAnswer', () => {
  it('turns the mock answer into numbered items and drops the repeated mock sentence', () => {
    const r = formatAnswer(MOCK, true, ctx);
    expect(r.items.map((i) => [i.n, i.label])).toEqual([[1, 'Pod not ready / Symptoms'], [2, 'Pod not ready / Overview']]);
    expect(JSON.stringify(r)).not.toContain('no language model was called');
  });

  it('renders inline code and splits flattened bullets', () => {
    const [first] = formatAnswer(MOCK, true, ctx).items;
    expect(first.html).toContain('<code>/readyz</code>');
    expect(first.html).toContain('•');
    expect(first.html).not.toContain('`');
  });

  it('renders any other answer (a real model) as Markdown', () => {
    const r = formatAnswer('Restart the pod [1].\n\n- check `oc get pods`', false, ctx);
    expect(r.items).toEqual([]);
    expect(r.html).toContain('<li>');
    expect(r.html).toContain('<code>oc get pods</code>');
  });

  it('never lets markup through', () => {
    expect(formatAnswer('<script>alert(1)</script> ok', false, ctx).html).not.toContain('<script');
  });
});
