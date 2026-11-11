import { Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { CatalogApi } from '../catalog/catalog-api';
import { Incident, IncidentApi, Severity } from '../incidents/incident-api';
import { relativeTime } from '../shared/relative-time';
import { StatusBadge } from '../shared/status-badge';

const SEVERITY_RANK: Record<Severity, number> = { SEV1: 0, SEV2: 1, SEV3: 2 };

/** The dashboard landing page: what is healthy, what is not, what needs attention now. */
@Component({
  selector: 'app-overview-page',
  imports: [RouterLink, StatusBadge],
  templateUrl: './overview-page.html',
  styles: `
    .service-grid { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fill, minmax(15rem, 1fr)); gap: 1rem; }
    .service-card { display: grid; gap: 0.5rem; padding: 1rem; }
    .service-card .top { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; }
    h2.section { font-size: 1.125rem; margin: 0 0 0.75rem; }
    .empty { padding: 1rem; margin: 0; color: var(--muted); }
  `,
})
export class OverviewPage {
  private readonly catalog = inject(CatalogApi);
  private readonly incidentApi = inject(IncidentApi);

  protected readonly services = rxResource({ stream: () => this.catalog.services() });
  protected readonly incidents = rxResource({ stream: () => this.incidentApi.list() });

  protected readonly failed = computed(() => !!this.services.error() || !!this.incidents.error());
  protected readonly loading = computed(() => this.services.isLoading() || this.incidents.isLoading());

  protected readonly counts = computed(() => {
    const all = this.services.value() ?? [];
    const count = (status: string) => all.filter((s) => s.status === status).length;
    return { up: count('UP'), degraded: count('DEGRADED'), down: count('DOWN') };
  });

  /** Open and mitigated incidents, most severe first, then newest first. */
  protected readonly active = computed(() =>
    (this.incidents.value() ?? [])
      .filter((i) => i.state !== 'RESOLVED')
      .sort((a, b) => SEVERITY_RANK[a.severity] - SEVERITY_RANK[b.severity] || b.openedAt.localeCompare(a.openedAt)),
  );
  protected readonly sev1 = computed(() => this.active().filter((i) => i.severity === 'SEV1').length);

  private readonly names = computed(() => new Map((this.services.value() ?? []).map((s) => [s.id, s.name])));

  protected serviceName(incident: Incident): string {
    return this.names().get(incident.serviceId) ?? `service ${incident.serviceId}`;
  }

  protected activeFor(serviceId: number): number {
    return this.active().filter((i) => i.serviceId === serviceId).length;
  }

  protected ago(iso: string): string {
    return relativeTime(iso);
  }

  protected reload(): void {
    this.services.reload();
    this.incidents.reload();
  }
}
