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
  });

  it('filters by state through a labelled select', async () => {
    const { fixture, el } = await render();
    expect(el.querySelector('label[for="state-filter"]')?.textContent).toContain('State');
    choose(el, '#state-filter', 'OPEN');
    fixture.detectChanges();
    TestBed.tick();
    http.expectOne('/api/incidents?state=OPEN').flush(INCIDENTS);
  });

  it('opens an incident and announces it', async () => {
    const { fixture, el } = await render();
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
  });

  it('does not send an empty form and points to the fields', async () => {
    const { fixture, el } = await render();
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    await settle(fixture);
    http.expectNone('/api/incidents');
    expect(el.querySelector('#service-error')?.textContent).toContain('Choose a service');
    expect(el.querySelector('#summary-error')?.textContent).toContain('Describe the problem');
    expect(el.querySelector('#service')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('shows the problem detail of a rejected request in an alert', async () => {
    const { fixture, el } = await render();
    choose(el, '#service', '1');
    choose(el, '#summary', 'x');
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    http.expectOne('/api/incidents').flush({ title: 'Unprocessable', status: 422, detail: 'Unknown service 1' }, { status: 422, statusText: 'Unprocessable' });
    await settle(fixture);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Unknown service 1');
  });

  it('has no axe violations', async () => {
    const { el } = await render();
    await expectNoAxeViolations(el);
  });
});
