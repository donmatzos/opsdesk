import { HttpErrorResponse } from '@angular/common/http';

/** RFC 9457 problem detail as sent by the API (`errors` only on validation failures). */
export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}

/** Angular resources wrap a thrown HttpErrorResponse in an Error (original in `cause`); this returns the original. */
export function unwrapError(error: unknown): unknown {
  return error instanceof Error && error.cause ? error.cause : error;
}

/** HTTP status of a failed request, if it was one. */
export function httpStatus(error: unknown): number | undefined {
  const e = unwrapError(error);
  return e instanceof HttpErrorResponse ? e.status : undefined;
}

/** A readable message from a failed request: the problem `detail` if there is one, else a generic text. */
export function problemMessage(failure: unknown, fallback = 'The request failed. Try again later.'): string {
  const error = unwrapError(failure);
  if (error instanceof HttpErrorResponse) {
    const body = error.error as ProblemDetail | null;
    if (body?.errors) {
      return Object.entries(body.errors).map(([field, message]) => `${field}: ${message}`).join('. ');
    }
    if (body?.detail) {
      return body.detail;
    }
    if (error.status === 0) {
      return 'The server is not reachable. Try again later.';
    }
  }
  return fallback;
}
