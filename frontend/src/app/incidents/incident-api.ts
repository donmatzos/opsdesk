import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type Severity = 'SEV1' | 'SEV2' | 'SEV3';
export type IncidentState = 'OPEN' | 'MITIGATED' | 'RESOLVED';

export interface Incident {
  id: number;
  serviceId: number;
  severity: Severity;
  state: IncidentState;
  summary: string;
  openedAt: string;
  resolvedAt: string | null;
}

export interface CreateIncident {
  serviceId: number;
  severity: Severity;
  summary: string;
}

export const SEVERITIES: Severity[] = ['SEV1', 'SEV2', 'SEV3'];
export const STATES: IncidentState[] = ['OPEN', 'MITIGATED', 'RESOLVED'];

/** Mirrors IncidentState.canTransitionTo in the backend: only forward, RESOLVED is final. */
export function allowedTransitions(state: IncidentState): IncidentState[] {
  switch (state) {
    case 'OPEN':
      return ['MITIGATED', 'RESOLVED'];
    case 'MITIGATED':
      return ['RESOLVED'];
    default:
      return [];
  }
}

@Injectable({ providedIn: 'root' })
export class IncidentApi {
  private readonly http = inject(HttpClient);

  list(state?: IncidentState): Observable<Incident[]> {
    return this.http.get<Incident[]>('/api/incidents', { params: state ? { state } : {} });
  }

  get(id: number): Observable<Incident> {
    return this.http.get<Incident>(`/api/incidents/${id}`);
  }

  open(request: CreateIncident): Observable<Incident> {
    return this.http.post<Incident>('/api/incidents', request);
  }

  changeState(id: number, state: IncidentState): Observable<Incident> {
    return this.http.patch<Incident>(`/api/incidents/${id}`, { state });
  }
}
