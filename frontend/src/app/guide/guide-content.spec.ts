import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { GuideContent } from './guide-content';

describe('GuideContent', () => {
  it('loads Markdown as text from a base-relative url', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    let body = '';
    TestBed.inject(GuideContent).load('architecture.md').subscribe((t) => (body = t));
    const req = http.expectOne('guide/architecture.md');
    expect(req.request.responseType).toBe('text');
    req.flush('# Architecture');
    expect(body).toBe('# Architecture');
    http.verify();
  });
});
