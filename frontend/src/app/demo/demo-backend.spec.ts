import { DemoBackend } from './demo-backend';

const NOW = new Date('2026-11-11T12:00:00Z');
const backend = () => new DemoBackend(() => NOW);
const problemOf = (r: { body: unknown }) => r.body as { status: number; detail: string; errors?: Record<string, string> };

describe('DemoBackend', () => {
  it('lists the four demo services sorted by name', () => {
    const r = backend().handle('GET', '/api/services', null);
    expect(r.status).toBe(200);
    expect((r.body as { name: string }[]).map((s) => s.name)).toEqual(['billing-api', 'customer-portal', 'order-service', 'report-batch']);
  });

  it('has incidents in every state and severity, newest first, and filters by state', () => {
    const b = backend();
    const all = b.handle('GET', '/api/incidents', null).body as { state: string; severity: string; openedAt: string }[];
    expect(all.length).toBe(5);
    expect(new Set(all.map((i) => i.state))).toEqual(new Set(['OPEN', 'MITIGATED', 'RESOLVED']));
    expect(new Set(all.map((i) => i.severity))).toEqual(new Set(['SEV1', 'SEV2', 'SEV3']));
    expect(all.map((i) => i.openedAt)).toEqual([...all.map((i) => i.openedAt)].sort().reverse());
    const open = b.handle('GET', '/api/incidents?state=OPEN', null).body as { state: string }[];
    expect(open.map((i) => i.state)).toEqual(['OPEN', 'OPEN']);
    expect(b.handle('GET', '/api/incidents?state=BOGUS', null).status).toBe(400);
  });

  it('moves OPEN -> MITIGATED -> RESOLVED and sets resolvedAt', () => {
    const b = backend();
    const first = b.handle('PATCH', '/api/incidents/1', { state: 'MITIGATED' });
    expect(first.status).toBe(200);
    const second = b.handle('PATCH', '/api/incidents/1', { state: 'RESOLVED' });
    expect((second.body as { state: string; resolvedAt: string }).resolvedAt).toBe(NOW.toISOString());
    expect(b.handle('PATCH', '/api/incidents/3', { state: 'RESOLVED' }).status).toBe(200); // OPEN -> RESOLVED directly
  });

  it('rejects a backwards transition with 409 and the real message', () => {
    const r = backend().handle('PATCH', '/api/incidents/4', { state: 'OPEN' });
    expect(r.status).toBe(409);
    expect(problemOf(r).detail).toBe('Cannot change incident state from RESOLVED to OPEN');
  });

  it('answers 404 for an unknown incident', () => {
    const b = backend();
    expect(problemOf(b.handle('GET', '/api/incidents/99', null))).toMatchObject({ status: 404, detail: 'Incident 99 not found' });
    expect(b.handle('PATCH', '/api/incidents/99', { state: 'RESOLVED' }).status).toBe(404);
  });

  it('opens an incident, and validates like the real API', () => {
    const b = backend();
    const created = b.handle('POST', '/api/incidents', { serviceId: 1, severity: 'SEV2', summary: '  slow  ' });
    expect(created.status).toBe(201);
    expect(created.body).toMatchObject({ id: 6, state: 'OPEN', summary: 'slow', openedAt: NOW.toISOString(), resolvedAt: null });
    expect((b.handle('GET', '/api/incidents', null).body as unknown[]).length).toBe(6);

    expect(problemOf(b.handle('POST', '/api/incidents', { serviceId: 1, severity: 'SEV2', summary: '   ' })).errors).toEqual({ summary: 'must not be blank' });
    expect(problemOf(b.handle('POST', '/api/incidents', { serviceId: 1, severity: 'SEV2', summary: 'x'.repeat(201) })).errors?.['summary']).toContain('size');
    expect(problemOf(b.handle('POST', '/api/incidents', { serviceId: 999, severity: 'SEV2', summary: 'x' }))).toMatchObject({ status: 422, detail: 'Service 999 does not exist' });
  });

  it('answers the assistant from canned runbook sections, with sources and a mock flag', () => {
    const r = backend().handle('POST', '/api/assist', { question: 'database connection refused' });
    const body = r.body as { answer: string; mock: boolean; sources: { runbook: string; path: string }[] };
    expect(body.mock).toBe(true);
    expect(body.answer).toContain('MOCK answer');
    expect(body.sources[0]).toMatchObject({ runbook: 'database-unavailable', path: 'docs/runbooks/database-unavailable.md' });
  });

  it('says no runbook covers an unknown question, with no sources', () => {
    const body = backend().handle('POST', '/api/assist', { question: 'xyzzy quux' }).body as { answer: string; sources: unknown[]; model: string };
    expect(body).toMatchObject({ answer: 'No runbook covers this question yet.', sources: [], model: 'none' });
    expect(backend().handle('POST', '/api/assist', { question: ' ' }).status).toBe(400);
  });

  it('gives each backend its own state', () => {
    const a = backend();
    a.handle('PATCH', '/api/incidents/1', { state: 'RESOLVED' });
    expect((backend().handle('GET', '/api/incidents/1', null).body as { state: string }).state).toBe('OPEN');
  });
});
