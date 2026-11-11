import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { expectNoAxeViolations } from '../../testing/axe';
import { Incident } from './incident-api';
import { IncidentDetailPage } from './incident-detail-page';

const MITIGATED: Incident = { id: 7, serviceId: 2, severity: 'SEV1', state: 'MITIGATED', summary: 'batch is down', openedAt: '2026-11-10T10:00:00Z', resolvedAt: null };

describe('IncidentDetailPage', () => {
  let http: HttpTestingController;

  async function render(answer: () => void) {
    TestBed.configureTestingModule({
      imports: [IncidentDetailPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(IncidentDetailPage);
    fixture.componentRef.setInput('id', '7');
    fixture.detectChanges();
    TestBed.tick();
    answer();
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  const ok = () => {
    http.expectOne('/api/services').flush([{ id: 2, name: 'report-batch', ownerTeam: 'team-data', stage: 'DEV', status: 'DOWN' }]);
    http.expectOne('/api/incidents/7').flush(MITIGATED);
  };

  afterEach(() => http.verify());

  it('lists the fields and offers only the allowed transition', async () => {
    const { el } = await render(ok);
    expect(el.querySelector('dl')?.textContent).toContain('report-batch');
    const buttons = Array.from(el.querySelectorAll('button')).map((b) => b.textContent?.trim());
    expect(buttons).toEqual(['Resolve']);
  });

  it('changes the state and announces the result', async () => {
    const { fixture, el } = await render(ok);
    (el.querySelector('button') as HTMLButtonElement).click();
    const patch = http.expectOne('/api/incidents/7');
    expect(patch.request.body).toEqual({ state: 'RESOLVED' });
    patch.flush({ ...MITIGATED, state: 'RESOLVED', resolvedAt: '2026-11-10T12:00:00Z' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(el.querySelector('[aria-live="polite"]')?.textContent).toContain('Incident #7 is now RESOLVED');
    expect(el.querySelectorAll('button').length).toBe(0);
  });

  it('shows the problem detail when a change is rejected', async () => {
    const { fixture, el } = await render(ok);
    (el.querySelector('button') as HTMLButtonElement).click();
    http.expectOne('/api/incidents/7').flush({ detail: 'Cannot change incident state from RESOLVED to OPEN' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Cannot change incident state');
  });

  it('says the incident was not found on 404', async () => {
    const { el } = await render(() => {
      http.expectOne('/api/services').flush([]);
      http.expectOne('/api/incidents/7').flush({ detail: 'Incident 7 not found' }, { status: 404, statusText: 'Not Found' });
    });
    expect(el.textContent).toContain('Incident not found');
    expect(el.querySelector('a')?.getAttribute('href')).toBe('/incidents');
  });

  it('has no axe violations', async () => {
    const { el } = await render(ok);
    await expectNoAxeViolations(el);
  });
});
