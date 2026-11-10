import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface MonitoredService {
  id: number;
  name: string;
  ownerTeam: string;
  stage: 'DEV' | 'PROD';
  status: 'UP' | 'DEGRADED' | 'DOWN';
}

@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly http = inject(HttpClient);

  services(): Observable<MonitoredService[]> {
    return this.http.get<MonitoredService[]>('/api/services');
  }
}
