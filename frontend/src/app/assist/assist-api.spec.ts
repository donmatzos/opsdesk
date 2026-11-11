import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AssistApi } from './assist-api';

describe('AssistApi', () => {
  it('posts the question to /api/assist', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    TestBed.inject(AssistApi).ask('pod not ready').subscribe();
    const req = http.expectOne('/api/assist');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ question: 'pod not ready' });
    req.flush({ answer: 'x', sources: [], model: 'mock', mock: true });
    http.verify();
  });
});
