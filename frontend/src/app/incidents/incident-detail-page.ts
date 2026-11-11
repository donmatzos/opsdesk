import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, numberAttribute, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { CatalogApi } from '../catalog/catalog-api';
import { httpStatus, problemMessage } from '../shared/problem';
import { StatusBadge } from '../shared/status-badge';
import { IncidentApi, IncidentState, allowedTransitions } from './incident-api';

const ACTION_LABEL: Record<IncidentState, string> = {
  OPEN: 'Reopen',
  MITIGATED: 'Mark as mitigated',
  RESOLVED: 'Resolve',
};

@Component({
  selector: 'app-incident-detail-page',
  imports: [DatePipe, RouterLink, StatusBadge],
  template: `
    <p><a routerLink="/incidents">All incidents</a></p>
    @if (incident.isLoading()) {
      <p class="state-text" role="status">Loading incident…</p>
    } @else if (notFound()) {
      <h1>Incident not found</h1>
      <p>There is no incident with the id {{ id() }}. <a routerLink="/incidents">Back to the incident list</a>.</p>
    } @else if (incident.error()) {
      <div class="alert" role="alert">
        <p>The incident could not be loaded.</p>
        <button type="button" (click)="incident.reload()">Try again</button>
      </div>
    } @else if (incident.value(); as current) {
      <h1>Incident #{{ current.id }}</h1>
      <dl class="details">
        <dt>Service</dt>
        <dd class="mono">{{ serviceName() }}</dd>
        <dt>Severity</dt>
        <dd><app-status-badge [value]="current.severity" /></dd>
        <dt>State</dt>
        <dd><app-status-badge [value]="current.state" /></dd>
        <dt>Summary</dt>
        <dd>{{ current.summary }}</dd>
        <dt>Opened</dt>
        <dd class="mono">{{ current.openedAt | date: 'yyyy-MM-dd HH:mm' }}</dd>
        <dt>Resolved</dt>
        <dd class="mono">{{ current.resolvedAt ? (current.resolvedAt | date: 'yyyy-MM-dd HH:mm') : 'not yet' }}</dd>
      </dl>

      @if (transitions().length) {
        <h2>Change state</h2>
        <p class="hint">Incidents only move forward; a resolved incident is never reopened.</p>
        <p>
          @for (next of transitions(); track next) {
            <button type="button" [class.primary]="next === 'RESOLVED'" [disabled]="busy()" (click)="change(next)">
              {{ label(next) }}
            </button>
          }
        </p>
      }
    }

    @if (error()) {
      <div class="alert" role="alert">{{ error() }}</div>
    }
    <div aria-live="polite">
      @if (announcement()) {
        <p class="notice">{{ announcement() }}</p>
      }
    </div>
  `,
  styles: `
    .details { display: grid; grid-template-columns: max-content 1fr; gap: 0.5rem 1.5rem; max-width: 48rem; }
    dt { color: var(--muted); font-weight: 600; }
    dd { margin: 0; }
    .hint { color: var(--muted); font-size: 0.875rem; }
    button + button { margin-left: 0.5rem; }
  `,
})
export class IncidentDetailPage {
  private readonly api = inject(IncidentApi);
  private readonly catalog = inject(CatalogApi);

  /** Bound from the route parameter (withComponentInputBinding). */
  readonly id = input.required({ transform: numberAttribute });

  protected readonly incident = rxResource({ params: () => this.id(), stream: ({ params }) => this.api.get(params) });
  private readonly services = rxResource({ stream: () => this.catalog.services() });

  protected readonly notFound = computed(() => httpStatus(this.incident.error()) === 404);
  protected readonly transitions = computed(() => {
    const current = this.incident.value();
    return current ? allowedTransitions(current.state) : [];
  });
  protected readonly serviceName = computed(() => {
    const serviceId = this.incident.value()?.serviceId;
    return this.services.value()?.find((s) => s.id === serviceId)?.name ?? `service ${serviceId}`;
  });

  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly announcement = signal('');

  protected label(state: IncidentState): string {
    return ACTION_LABEL[state];
  }

  protected change(next: IncidentState): void {
    this.busy.set(true);
    this.error.set('');
    this.announcement.set('');
    this.api.changeState(this.id(), next).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.incident.set(updated);
        this.announcement.set(`Incident #${updated.id} is now ${updated.state}.`);
      },
      error: (e: unknown) => {
        this.busy.set(false);
        this.error.set(problemMessage(e));
      },
    });
  }
}
