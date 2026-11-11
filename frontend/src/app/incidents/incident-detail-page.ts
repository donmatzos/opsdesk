import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, numberAttribute, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { CatalogApi } from '../catalog/catalog-api';
import { httpStatus, problemMessage } from '../shared/problem';
import { StatusBadge } from '../shared/status-badge';
import { relativeTime } from '../shared/relative-time';
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
    <p class="back"><a routerLink="/incidents">All incidents</a></p>
    @if (incident.isLoading()) {
      <output class="state-text">Loading incident…</output>
    } @else if (notFound()) {
      <h1>Incident not found</h1>
      <p>There is no incident with the id {{ id() }}. <a routerLink="/incidents">Back to the incident list</a>.</p>
    } @else if (incident.error()) {
      <div class="alert" role="alert">
        <p>The incident could not be loaded.</p>
        <button type="button" (click)="incident.reload()">Try again</button>
      </div>
    } @else if (incident.value(); as current) {
      <header class="page-header head">
        <div>
          <h1>Incident #{{ current.id }}</h1>
          <p class="lede">{{ current.summary }}</p>
        </div>
        <span class="badges">
          <app-status-badge [value]="current.severity" />
          <app-status-badge [value]="current.state" />
        </span>
      </header>

      <div class="columns">
        <section class="card" aria-labelledby="details-heading">
          <div class="card-head"><h2 id="details-heading">Details</h2></div>
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
        </section>

        <section class="card" aria-labelledby="timeline-heading">
          <div class="card-head"><h2 id="timeline-heading">Timeline</h2></div>
          <ol class="timeline">
            <li>
              <strong>Opened</strong> <time [attr.datetime]="current.openedAt">{{ ago(current.openedAt) }}</time>
              <span class="meta">{{ current.openedAt | date: 'yyyy-MM-dd HH:mm' }}</span>
            </li>
            @if (current.resolvedAt) {
              <li>
                <strong>Resolved</strong> <time [attr.datetime]="current.resolvedAt">{{ ago(current.resolvedAt) }}</time>
                <span class="meta">{{ current.resolvedAt | date: 'yyyy-MM-dd HH:mm' }}</span>
              </li>
            } @else {
              <li><strong>Now:</strong> {{ current.state }}</li>
            }
          </ol>
          @if (transitions().length) {
            <div class="card-body">
              <h3 class="small">Change state</h3>
              <p class="hint">Incidents only move forward; a resolved incident is never reopened.</p>
              <p class="actions">
                @for (next of transitions(); track next) {
                  <button type="button" [class.primary]="next === 'RESOLVED'" [disabled]="busy()" (click)="change(next)">
                    {{ label(next) }}
                  </button>
                }
              </p>
            </div>
          }
        </section>
      </div>
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
    .columns { display: grid; gap: 1.5rem; grid-template-columns: minmax(0, 3fr) minmax(0, 2fr); align-items: start; }
    @media (max-width: 899px) { .columns { grid-template-columns: minmax(0, 1fr); } }
    .badges { display: inline-flex; gap: 0.5rem; }
    .details { display: grid; grid-template-columns: max-content 1fr; gap: 0.75rem 1.5rem; margin: 0; padding: 0 1rem 1rem; }
    dt { color: var(--muted); font-weight: 600; }
    dd { margin: 0; }
    .timeline { list-style: none; margin: 0; padding: 0 1rem 1rem; display: grid; gap: 1rem; }
    .timeline li { display: grid; gap: 0.125rem; padding-left: 1rem; border-left: 3px solid var(--accent); }
    .small { margin: 0 0 0.25rem; font-size: 1rem; }
    .hint { color: var(--muted); font-size: 0.875rem; margin: 0 0 0.75rem; }
    .actions { margin: 0; display: flex; gap: 0.5rem; flex-wrap: wrap; }
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

  protected ago(iso: string): string {
    return relativeTime(iso);
  }

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
