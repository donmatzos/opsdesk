import { Component, Injector, afterNextRender, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { CatalogApi } from '../catalog/catalog-api';
import { problemMessage } from '../shared/problem';
import { relativeTime } from '../shared/relative-time';
import { StatusBadge } from '../shared/status-badge';
import { IncidentApi, IncidentState, SEVERITIES, STATES, Severity } from './incident-api';

type Filter = 'ALL' | IncidentState;

const FILTERS: { value: Filter; label: string }[] = [
  { value: 'ALL', label: 'All' },
  ...STATES.map((s) => ({ value: s as Filter, label: s.charAt(0) + s.slice(1).toLowerCase() })),
];

@Component({
  selector: 'app-incidents-page',
  imports: [RouterLink, StatusBadge],
  templateUrl: './incidents-page.html',
})
export class IncidentsPage {
  private readonly api = inject(IncidentApi);
  private readonly catalog = inject(CatalogApi);
  private readonly injector = inject(Injector);

  protected readonly severities = SEVERITIES;
  protected readonly filters = FILTERS;

  protected readonly filter = signal<Filter>('ALL');
  protected readonly incidents = rxResource({ stream: () => this.api.list() });
  protected readonly services = rxResource({ stream: () => this.catalog.services() });
  protected readonly serviceNames = computed(() => new Map((this.services.value() ?? []).map((s) => [s.id, s.name])));

  protected readonly counts = computed(() => {
    const all = this.incidents.value() ?? [];
    return {
      ALL: all.length,
      OPEN: all.filter((i) => i.state === 'OPEN').length,
      MITIGATED: all.filter((i) => i.state === 'MITIGATED').length,
      RESOLVED: all.filter((i) => i.state === 'RESOLVED').length,
    };
  });
  protected readonly visible = computed(() =>
    (this.incidents.value() ?? []).filter((i) => this.filter() === 'ALL' || i.state === this.filter()),
  );

  protected readonly formOpen = signal(false);
  protected readonly serviceId = signal('');
  protected readonly severity = signal<Severity>('SEV3');
  protected readonly summary = signal('');
  protected readonly errors = signal<{ service?: string; summary?: string }>({});
  protected readonly serverError = signal('');
  protected readonly announcement = signal('');
  protected readonly submitting = signal(false);

  protected ago(iso: string): string {
    return relativeTime(iso);
  }

  protected toggleForm(): void {
    const open = !this.formOpen();
    this.formOpen.set(open);
    this.errors.set({});
    this.serverError.set('');
    if (open) {
      afterNextRender(() => document.getElementById('service')?.focus(), { injector: this.injector });
    }
  }

  protected submit(event: Event): void {
    event.preventDefault();
    this.serverError.set('');
    this.announcement.set('');
    const errors: { service?: string; summary?: string } = {};
    if (!this.serviceId()) {
      errors.service = 'Choose a service.';
    }
    if (!this.summary().trim()) {
      errors.summary = 'Describe the problem in a few words.';
    }
    this.errors.set(errors);
    if (errors.service || errors.summary) {
      document.getElementById(errors.service ? 'service' : 'summary')?.focus();
      return;
    }
    this.submitting.set(true);
    this.api.open({ serviceId: Number(this.serviceId()), severity: this.severity(), summary: this.summary().trim() }).subscribe({
      next: (created) => {
        this.submitting.set(false);
        this.summary.set('');
        this.formOpen.set(false);
        this.announcement.set(`Incident #${created.id} opened.`);
        this.incidents.reload();
      },
      error: (e: unknown) => {
        this.submitting.set(false);
        this.serverError.set(problemMessage(e));
      },
    });
  }
}
