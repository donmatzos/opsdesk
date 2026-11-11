import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { IncidentApi, allowedTransitions } from './incident-api';

describe('IncidentApi', () => {
  let api: IncidentApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(IncidentApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists with and without a state filter', () => {
    api.list('OPEN').subscribe();
    const filtered = http.expectOne('/api/incidents?state=OPEN');
    expect(filtered.request.method).toBe('GET');
    filtered.flush([]);
    api.list().subscribe();
    const unfiltered = http.expectOne('/api/incidents');
    expect(unfiltered.request.params.has('state')).toBe(false);
    unfiltered.flush([]);
  });

  it('opens an incident with POST and changes state with PATCH', () => {
    api.open({ serviceId: 1, severity: 'SEV2', summary: 'slow' }).subscribe();
    const post = http.expectOne('/api/incidents');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ serviceId: 1, severity: 'SEV2', summary: 'slow' });
    post.flush({});

    api.changeState(7, 'RESOLVED').subscribe();
    const patch = http.expectOne('/api/incidents/7');
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ state: 'RESOLVED' });
    patch.flush({});
  });

  it('only allows forward transitions', () => {
    expect(allowedTransitions('OPEN')).toEqual(['MITIGATED', 'RESOLVED']);
    expect(allowedTransitions('MITIGATED')).toEqual(['RESOLVED']);
    expect(allowedTransitions('RESOLVED')).toEqual([]);
  });
});
