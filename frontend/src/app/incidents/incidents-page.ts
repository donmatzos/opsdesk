import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { CatalogApi } from '../catalog/catalog-api';
import { problemMessage } from '../shared/problem';
import { StatusBadge } from '../shared/status-badge';
import { IncidentApi, IncidentState, SEVERITIES, STATES, Severity } from './incident-api';

@Component({
  selector: 'app-incidents-page',
  imports: [DatePipe, RouterLink, StatusBadge],
  templateUrl: './incidents-page.html',
})
export class IncidentsPage {
  private readonly api = inject(IncidentApi);
  private readonly catalog = inject(CatalogApi);

  protected readonly states = STATES;
  protected readonly severities = SEVERITIES;

  protected readonly filter = signal<IncidentState | ''>('');
  protected readonly incidents = rxResource({
    params: () => this.filter(),
    stream: ({ params }) => this.api.list(params || undefined),
  });
  protected readonly services = rxResource({ stream: () => this.catalog.services() });
  protected readonly serviceNames = computed(() => new Map((this.services.value() ?? []).map((s) => [s.id, s.name])));

  protected readonly serviceId = signal('');
  protected readonly severity = signal<Severity>('SEV3');
  protected readonly summary = signal('');
  protected readonly errors = signal<{ service?: string; summary?: string }>({});
  protected readonly serverError = signal('');
  protected readonly announcement = signal('');
  protected readonly submitting = signal(false);

  protected setFilter(value: string): void {
    this.filter.set(value as IncidentState | '');
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
