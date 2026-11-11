import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { DEMO_LATENCY, apiInterceptors, demoApiInterceptor } from './demo-api.interceptor';

describe('demoApiInterceptor', () => {
  function setup() {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([demoApiInterceptor])),
        provideHttpClientTesting(),
        { provide: DEMO_LATENCY, useValue: () => 0 },
      ],
    });
    return { client: TestBed.inject(HttpClient), http: TestBed.inject(HttpTestingController) };
  }

  it('answers /api requests from memory without any network request', async () => {
    const { client, http } = setup();
    const services = await firstValueFrom(client.get<unknown[]>('/api/services'));
    expect(services.length).toBe(4);
    http.expectNone(() => true);
  });

  it('turns problem responses into HttpErrorResponse with the problem body', async () => {
    const { client } = setup();
    const error = await firstValueFrom(client.get('/api/incidents/99')).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(HttpErrorResponse);
    expect((error as HttpErrorResponse).status).toBe(404);
    expect((error as HttpErrorResponse).error.detail).toBe('Incident 99 not found');
  });

  it('keeps state between requests', async () => {
    const { client } = setup();
    await firstValueFrom(client.post('/api/incidents', { serviceId: 1, severity: 'SEV3', summary: 'x' }));
    const all = await firstValueFrom(client.get<unknown[]>('/api/incidents'));
    expect(all.length).toBe(6);
  });

  it('lets requests outside /api pass through to the network', () => {
    const { client, http } = setup();
    let body = '';
    client.get('guide/index.md', { responseType: 'text' }).subscribe((text) => (body = text));
    http.expectOne('guide/index.md').flush('# x');
    expect(body).toBe('# x');
    http.verify();
  });

  it('is only registered for the demo configuration, never in the jar build', () => {
    expect(environment.demo).toBe(false);
    expect(apiInterceptors(false)).toEqual([]);
    expect(apiInterceptors(true)).toEqual([demoApiInterceptor]);
  });
});
