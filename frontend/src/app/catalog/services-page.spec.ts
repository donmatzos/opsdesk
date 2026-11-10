import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ServicesPage } from './services-page';
import { MonitoredService } from './catalog-api';

describe('ServicesPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ServicesPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('renders one row per service with name and status', async () => {
    const fixture = TestBed.createComponent(ServicesPage);
    fixture.detectChanges();

    const services: MonitoredService[] = [
      { id: 1, name: 'billing-api', ownerTeam: 'team-payments', stage: 'PROD', status: 'UP' },
      { id: 2, name: 'report-batch', ownerTeam: 'team-data', stage: 'DEV', status: 'DOWN' },
    ];
    http.expectOne('/api/services').flush(services);
    await fixture.whenStable();

    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('billing-api');
    expect(rows[0].textContent).toContain('UP');
    expect(rows[1].textContent).toContain('DOWN');
  });
});
