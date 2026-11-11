import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { expectNoAxeViolations } from '../../testing/axe';
import { MonitoredService } from '../catalog/catalog-api';
import { Incident } from '../incidents/incident-api';
import { OverviewPage } from './overview-page';

const SERVICES: MonitoredService[] = [
  { id: 1, name: 'billing-api', ownerTeam: 'team-payments', stage: 'PROD', status: 'UP' },
  { id: 2, name: 'customer-portal', ownerTeam: 'team-web', stage: 'PROD', status: 'DEGRADED' },
  { id: 3, name: 'report-batch', ownerTeam: 'team-data', stage: 'DEV', status: 'DOWN' },
];
const now = Date.now();
const iso = (hoursAgo: number) => new Date(now - hoursAgo * 3_600_000).toISOString();
const INCIDENTS: Incident[] = [
  { id: 1, serviceId: 2, severity: 'SEV3', state: 'OPEN', summary: 'slow checkout', openedAt: iso(30), resolvedAt: null },
  { id: 2, serviceId: 3, severity: 'SEV1', state: 'OPEN', summary: 'batch is down', openedAt: iso(3), resolvedAt: null },
  { id: 3, serviceId: 1, severity: 'SEV2', state: 'MITIGATED', summary: 'invoice errors', openedAt: iso(5), resolvedAt: null },
  { id: 4, serviceId: 1, severity: 'SEV1', state: 'RESOLVED', summary: 'old outage', openedAt: iso(90), resolvedAt: iso(88) },
];

describe('OverviewPage', () => {
  let http: HttpTestingController;

  function render() {
    TestBed.configureTestingModule({ imports: [OverviewPage], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(OverviewPage);
    fixture.detectChanges();
    TestBed.tick();
    return fixture;
  }

  async function load(fixture: ReturnType<typeof render>, incidents: Incident[] = INCIDENTS) {
    http.expectOne('/api/services').flush(SERVICES);
    http.expectOne('/api/incidents').flush(incidents);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => http.verify());

  it('shows status counts and the number of active incidents as tiles', async () => {
    const el = await load(render());
    const tiles = Array.from(el.querySelectorAll('.tile')).map((t) => [
      t.querySelector('.tile-label')?.textContent,
      t.querySelector('.tile-value')?.textContent,
      t.querySelector('.tile-hint')?.textContent,
    ]);
    expect(tiles).toEqual([
      ['Up', '1', 'services'],
      ['Degraded', '1', 'services'],
      ['Down', '1', 'services'],
      ['Active incidents', '3', '1 SEV1'],
    ]);
  });

  it('lists active incidents by severity, then newest, and leaves resolved ones out', async () => {
    const el = await load(render());
    const rows = Array.from(el.querySelectorAll('.attention .row')).map((r) => r.textContent?.replace(/\s+/g, ' ').trim() ?? '');
    expect(rows.length).toBe(3);
    expect(rows[0]).toContain('batch is down');
    expect(rows[1]).toContain('invoice errors');
    expect(rows[2]).toContain('slow checkout');
    expect(el.textContent).not.toContain('old outage');
    expect(el.querySelector('.attention .row a')?.getAttribute('href')).toBe('/incidents/2');
  });

  it('shows a card per service with its active incidents', async () => {
    const el = await load(render());
    const cards = Array.from(el.querySelectorAll('.service-card'));
    expect(cards.length).toBe(3);
    expect(cards[0].textContent).toContain('billing-api');
    expect(cards[0].textContent).toContain('1 active incident');
    expect(cards[2].textContent).toContain('1 active incident');
  });

  it('says so when nothing needs attention', async () => {
    const el = await load(render(), [INCIDENTS[3]]);
    expect(el.textContent).toContain('Nothing needs attention');
  });

  it('shows an error with a retry button when a request fails', async () => {
    const fixture = render();
    http.expectOne('/api/services').flush('x', { status: 500, statusText: 'Error' });
    http.expectOne('/api/incidents').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent).toContain('could not be loaded');
  });

  it('has no axe violations', async () => {
    const el = await load(render());
    await expectNoAxeViolations(el);
  });
});
