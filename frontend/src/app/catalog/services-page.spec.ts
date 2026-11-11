import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { expectNoAxeViolations } from '../../testing/axe';
import { ServicesPage } from './services-page';
import { MonitoredService } from './catalog-api';

const SERVICES: MonitoredService[] = [
  { id: 1, name: 'billing-api', ownerTeam: 'team-payments', stage: 'PROD', status: 'UP' },
  { id: 2, name: 'report-batch', ownerTeam: 'team-data', stage: 'DEV', status: 'DOWN' },
  { id: 3, name: 'order-service', ownerTeam: 'team-orders', stage: 'PROD', status: 'UP' },
];

describe('ServicesPage', () => {
  let http: HttpTestingController;

  async function render() {
    TestBed.configureTestingModule({
      imports: [ServicesPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ServicesPage);
    fixture.detectChanges();
    TestBed.tick(); // runs the resource effect, which issues the request
    return fixture;
  }

  async function answer(fixture: Awaited<ReturnType<typeof render>>, services: MonitoredService[]) {
    http.expectOne('/api/services').flush(services);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => http.verify());

  it('shows a loading text first', async () => {
    const fixture = await render();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Loading services');
    http.expectOne('/api/services').flush([]);
  });

  it('summarises the statuses as text and renders one row per service with a badge', async () => {
    const el = await answer(await render(), SERVICES);
    expect(el.querySelector('h1')?.textContent).toBe('Services');
    expect(el.querySelector('.summary')?.textContent).toContain('3 services: 2 UP, 1 DOWN');
    expect(el.querySelector('caption')?.textContent).toContain('Monitored services');
    expect(el.querySelectorAll('th[scope="col"]').length).toBe(4);
    const rows = el.querySelectorAll('tbody tr');
    expect(rows.length).toBe(3);
    expect(rows[0].textContent).toContain('billing-api');
    expect(rows[1].querySelector('app-status-badge')?.textContent?.trim()).toBe('DOWN');
  });

  it('says so when there are no services', async () => {
    const el = await answer(await render(), []);
    expect(el.textContent).toContain('No services are monitored yet.');
  });

  it('shows an error with a retry button when the request fails', async () => {
    const fixture = await render();
    http.expectOne('/api/services').flush('boom', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Services could not be loaded');
    (el.querySelector('button') as HTMLButtonElement).click();
    fixture.detectChanges();
    TestBed.tick();
    http.expectOne('/api/services').flush([]);
  });

  it('has no axe violations', async () => {
    const el = await answer(await render(), SERVICES);
    await expectNoAxeViolations(el);
  });
});
