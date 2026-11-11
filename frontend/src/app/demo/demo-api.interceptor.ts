import { HttpErrorResponse, HttpHeaders, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { InjectionToken, inject } from '@angular/core';
import { of, switchMap, throwError, timer } from 'rxjs';
import { DEMO_BACKEND } from './demo-backend';

/** Simulated network latency in ms (150-300 by default); tests provide 0. */
export const DEMO_LATENCY = new InjectionToken<() => number>('DEMO_LATENCY', {
  providedIn: 'root',
  factory: () => () => 150 + Math.random() * 150,
});

/** Answers every /api/* request from the in-memory DemoBackend; everything else (guide assets, fonts) passes through. */
export const demoApiInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/')) {
    return next(req);
  }
  const result = inject(DEMO_BACKEND).handle(req.method, req.urlWithParams, req.body);
  const latency = inject(DEMO_LATENCY)();
  return timer(latency).pipe(
    switchMap(() =>
      result.status < 400
        ? of(new HttpResponse({ status: result.status, body: result.body, url: req.url }))
        : throwError(
            () =>
              new HttpErrorResponse({
                status: result.status,
                error: result.body,
                url: req.url,
                headers: new HttpHeaders({ 'Content-Type': 'application/problem+json' }),
              }),
          ),
    ),
  );
};

/** The jar build registers no interceptor at all; only the "demo" configuration (demo: true) does. */
export function apiInterceptors(demo: boolean) {
  return demo ? [demoApiInterceptor] : [];
}
