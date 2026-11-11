import { InjectionToken } from '@angular/core';
import { MonitoredService } from '../catalog/catalog-api';
import { AssistAnswer } from '../assist/assist-api';
import { CreateIncident, Incident, IncidentState, SEVERITIES, STATES, allowedTransitions } from '../incidents/incident-api';
import { DEMO_RUNBOOKS, DEMO_SERVICES, demoIncidents } from './demo-data';

export interface DemoResponse {
  status: number;
  body: unknown;
}

const TITLES: Record<number, string> = { 400: 'Bad Request', 404: 'Not Found', 409: 'Conflict', 422: 'Unprocessable Content' };

function problem(status: number, detail: string, extra: object = {}): DemoResponse {
  return { status, body: { type: 'about:blank', title: TITLES[status] ?? 'Error', status, detail, ...extra } };
}

/**
 * In-memory stand-in for the REST API, used only by the static demo. It follows the real API's rules: transitions only
 * go forward, errors are problem details (400 with field errors, 404, 409, 422), state resets on reload.
 */
export class DemoBackend {
  private readonly services: MonitoredService[] = DEMO_SERVICES.map((s) => ({ ...s }));
  private incidents: Incident[];
  private nextId: number;

  constructor(private readonly clock: () => Date = () => new Date()) {
    this.incidents = demoIncidents(clock());
    this.nextId = this.incidents.length + 1;
  }

  handle(method: string, urlWithParams: string, body: unknown): DemoResponse {
    const url = new URL(urlWithParams, 'https://demo.invalid');
    const path = url.pathname;
    const incident = /^\/api\/incidents\/(\d+)$/.exec(path);

    if (method === 'GET' && path === '/api/services') {
      return { status: 200, body: [...this.services].sort((a, b) => a.name.localeCompare(b.name)) };
    }
    if (method === 'GET' && path === '/api/incidents') {
      return this.list(url.searchParams.get('state'));
    }
    if (method === 'POST' && path === '/api/incidents') {
      return this.open(body as Partial<CreateIncident> | null);
    }
    if (incident && method === 'GET') {
      return this.find(Number(incident[1])) ?? this.notFound(Number(incident[1]));
    }
    if (incident && method === 'PATCH') {
      return this.changeState(Number(incident[1]), (body as { state?: string } | null)?.state);
    }
    if (method === 'POST' && path === '/api/assist') {
      return this.assist((body as { question?: string } | null)?.question);
    }
    return problem(404, `No demo endpoint for ${method} ${path}`);
  }

  private find(id: number): DemoResponse | undefined {
    const found = this.incidents.find((i) => i.id === id);
    return found ? { status: 200, body: found } : undefined;
  }

  private notFound(id: number): DemoResponse {
    return problem(404, `Incident ${id} not found`);
  }

  private list(state: string | null): DemoResponse {
    if (state && !STATES.includes(state as IncidentState)) {
      return problem(400, `Method parameter 'state': Failed to convert value '${state}'`);
    }
    const filtered = this.incidents.filter((i) => !state || i.state === state);
    return { status: 200, body: [...filtered].sort((a, b) => b.openedAt.localeCompare(a.openedAt)) };
  }

  private open(request: Partial<CreateIncident> | null): DemoResponse {
    const errors: Record<string, string> = {};
    if (request?.serviceId == null) {
      errors['serviceId'] = 'must not be null';
    }
    if (!request?.severity || !SEVERITIES.includes(request.severity)) {
      errors['severity'] = 'must not be null';
    }
    const summary = request?.summary ?? '';
    if (!summary.trim()) {
      errors['summary'] = 'must not be blank';
    } else if (summary.length > 200) {
      errors['summary'] = 'size must be between 0 and 200';
    }
    if (Object.keys(errors).length) {
      return problem(400, 'Invalid request content.', { errors });
    }
    if (!this.services.some((s) => s.id === request!.serviceId)) {
      return problem(422, `Service ${request!.serviceId} does not exist`);
    }
    const created: Incident = {
      id: this.nextId++,
      serviceId: request!.serviceId!,
      severity: request!.severity!,
      state: 'OPEN',
      summary: summary.trim(),
      openedAt: this.clock().toISOString(),
      resolvedAt: null,
    };
    this.incidents = [...this.incidents, created];
    return { status: 201, body: created };
  }

  private changeState(id: number, state: string | undefined): DemoResponse {
    const current = this.incidents.find((i) => i.id === id);
    if (!current) {
      return this.notFound(id);
    }
    if (!state || !STATES.includes(state as IncidentState)) {
      return problem(400, 'Invalid request content.', { errors: { state: 'must not be null' } });
    }
    const next = state as IncidentState;
    if (!allowedTransitions(current.state).includes(next)) {
      return problem(409, `Cannot change incident state from ${current.state} to ${next}`);
    }
    const updated: Incident = { ...current, state: next, resolvedAt: next === 'RESOLVED' ? this.clock().toISOString() : null };
    this.incidents = this.incidents.map((i) => (i.id === id ? updated : i));
    return { status: 200, body: updated };
  }

  private assist(question: string | undefined): DemoResponse {
    if (!question?.trim() || question.length > 500) {
      return problem(400, 'Invalid request content.', { errors: { question: 'must not be blank and at most 500 characters' } });
    }
    const words = new Set(question.toLowerCase().split(/[^a-z0-9]+/).filter(Boolean));
    const hits = DEMO_RUNBOOKS.map((rb) => ({ rb, score: rb.keywords.filter((k) => words.has(k)).length }))
      .filter((h) => h.score > 0)
      .sort((a, b) => b.score - a.score)
      .slice(0, 3);
    if (!hits.length) {
      const none: AssistAnswer = { answer: 'No runbook covers this question yet.', sources: [], model: 'none', mock: false };
      return { status: 200, body: none };
    }
    const answer: AssistAnswer = {
      answer:
        'MOCK answer (no language model was called). The most relevant runbook sections:\n' +
        hits.map((h, i) => `[${i + 1}] ${h.rb.title} / ${h.rb.section}: ${h.rb.excerpt}`).join('\n'),
      sources: hits.map((h) => ({ runbook: h.rb.runbook, title: h.rb.title, section: h.rb.section, path: `docs/runbooks/${h.rb.runbook}.md`, score: h.score })),
      model: 'mock',
      mock: true,
    };
    return { status: 200, body: answer };
  }
}

/** One backend per app (state lives for the page's lifetime); tests provide their own with a fixed clock. */
export const DEMO_BACKEND = new InjectionToken<DemoBackend>('DEMO_BACKEND', {
  providedIn: 'root',
  factory: () => new DemoBackend(),
});
