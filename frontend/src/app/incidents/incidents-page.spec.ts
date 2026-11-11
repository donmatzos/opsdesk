import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { expectNoAxeViolations } from '../../testing/axe';
import { MonitoredService } from '../catalog/catalog-api';
import { Incident } from './incident-api';
import { IncidentsPage } from './incidents-page';

const SERVICES: MonitoredService[] = [
  { id: 1, name: 'billing-api', ownerTeam: 'team-payments', stage: 'PROD', status: 'UP' },
  { id: 2, name: 'report-batch', ownerTeam: 'team-data', stage: 'DEV', status: 'DOWN' },
];
const INCIDENTS: Incident[] = [
  { id: 7, serviceId: 2, severity: 'SEV1', state: 'OPEN', summary: 'batch is down', openedAt: '2026-11-10T10:00:00Z', resolvedAt: null },
];

describe('IncidentsPage', () => {
  let http: HttpTestingController;

  async function settle(fixture: { whenStable(): Promise<unknown>; detectChanges(): void }) {
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function render() {
    TestBed.configureTestingModule({
      imports: [IncidentsPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(IncidentsPage);
    fixture.detectChanges();
    TestBed.tick();
    http.expectOne('/api/services').flush(SERVICES);
    http.expectOne('/api/incidents').flush(INCIDENTS);
    await settle(fixture);
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  function choose(el: HTMLElement, selector: string, value: string) {
    const control = el.querySelector(selector) as HTMLSelectElement | HTMLInputElement;
    control.value = value;
    control.dispatchEvent(new Event(control instanceof HTMLSelectElement ? 'change' : 'input'));
  }

  afterEach(() => http.verify());

  it('shows service names (not ids) and links each incident to its detail', async () => {
    const { el } = await render();
    const row = el.querySelector('tbody tr') as HTMLElement;
    expect(row.textContent).toContain('report-batch');
    expect(row.textContent).toContain('SEV1');
    expect((row.querySelector('a') as HTMLAnchorElement).getAttribute('href')).toBe('/incidents/7');
    expect(Array.from(row.querySelectorAll('td')).map((td) => td.getAttribute('data-label'))).toEqual(['Id', 'Service', 'Severity', 'State', 'Summary', 'Opened']);
  });

  it('filters by state with chips that show counts, without a new request', async () => {
    const { fixture, el } = await render();
    const chips = Array.from(el.querySelectorAll('.chip')).map((c) => c.textContent?.replace(/\s+/g, ' ').trim());
    expect(chips).toEqual(['All 1', 'Open 1', 'Mitigated 0', 'Resolved 0']);
    (Array.from(el.querySelectorAll('.chip')).find((c) => c.textContent?.includes('Resolved')) as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(el.textContent).toContain('No incidents match');
    expect(el.querySelector('.chip[aria-pressed="true"]')?.textContent).toContain('Resolved');
  });

  it('opens the form from a disclosure button and announces a new incident', async () => {
    const { fixture, el } = await render();
    const button = el.querySelector('button.disclosure') as HTMLButtonElement;
    expect(button.getAttribute('aria-expanded')).toBe('false');
    expect(el.querySelector('form')).toBeNull();
    button.click();
    fixture.detectChanges();
    expect(button.getAttribute('aria-expanded')).toBe('true');
    choose(el, '#service', '1');
    choose(el, '#severity', 'SEV2');
    choose(el, '#summary', 'checkout is slow');
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    const post = http.expectOne('/api/incidents');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ serviceId: 1, severity: 'SEV2', summary: 'checkout is slow' });
    post.flush({ id: 8, serviceId: 1, severity: 'SEV2', state: 'OPEN', summary: 'checkout is slow', openedAt: '2026-11-10T11:00:00Z', resolvedAt: null });
    fixture.detectChanges();
    TestBed.tick();
    http.expectOne('/api/incidents').flush([...INCIDENTS, { id: 8, serviceId: 1, severity: 'SEV2', state: 'OPEN', summary: 'checkout is slow', openedAt: '2026-11-10T11:00:00Z', resolvedAt: null }]);
    await settle(fixture);
    expect(el.querySelectorAll('tbody tr').length).toBe(2);
    expect(el.querySelector('[aria-live="polite"]')?.textContent).toContain('Incident #8 opened');
    expect(el.querySelector('form')).toBeNull();
  });

  async function openForm(fixture: { detectChanges(): void }, el: HTMLElement) {
    (el.querySelector('button.disclosure') as HTMLButtonElement).click();
    fixture.detectChanges();
  }

  it('does not send an empty form and points to the fields', async () => {
    const { fixture, el } = await render();
    await openForm(fixture, el);
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    await settle(fixture);
    http.expectNone('/api/incidents');
    expect(el.querySelector('#service-error')?.textContent).toContain('Choose a service');
    expect(el.querySelector('#summary-error')?.textContent).toContain('Describe the problem');
    expect(el.querySelector('#service')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('shows the problem detail of a rejected request in an alert', async () => {
    const { fixture, el } = await render();
    await openForm(fixture, el);
    choose(el, '#service', '1');
    choose(el, '#summary', 'x');
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    http.expectOne('/api/incidents').flush({ title: 'Unprocessable', status: 422, detail: 'Unknown service 1' }, { status: 422, statusText: 'Unprocessable' });
    await settle(fixture);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Unknown service 1');
  });

  it('has no axe violations, with the form open too', async () => {
    const { fixture, el } = await render();
    await expectNoAxeViolations(el);
    await openForm(fixture, el);
    await expectNoAxeViolations(el);
  });
});
