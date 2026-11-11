import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

// The only file that knows the /api/assist shape (backend: AssistRequest / AssistResponse in package runbook).
export interface AssistSource {
  runbook: string;
  title: string;
  section: string;
  path: string;
  score: number;
}

export interface AssistAnswer {
  answer: string;
  sources: AssistSource[];
  model: string;
  /** true when no real language model answered (the default, see docs/context/cloud-mocks.md) */
  mock: boolean;
}

export const MAX_QUESTION_LENGTH = 500;

@Injectable({ providedIn: 'root' })
export class AssistApi {
  private readonly http = inject(HttpClient);

  ask(question: string): Observable<AssistAnswer> {
    return this.http.post<AssistAnswer>('/api/assist', { question });
  }
}
