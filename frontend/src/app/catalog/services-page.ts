import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { StatusBadge } from '../shared/status-badge';
import { CatalogApi, MonitoredService } from './catalog-api';

type Filter = 'ALL' | MonitoredService['status'];

const FILTERS: { value: Filter; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'UP', label: 'Up' },
  { value: 'DEGRADED', label: 'Degraded' },
  { value: 'DOWN', label: 'Down' },
];

@Component({
  selector: 'app-services-page',
  imports: [StatusBadge],
  template: `
    <header class="page-header">
      <h1>Services</h1>
      <p class="lede">Every monitored service with its owner, stage and current status.</p>
    </header>

    @if (services.isLoading()) {
      <output class="state-text">Loading services…</output>
    } @else if (services.error()) {
      <div class="alert" role="alert">
        <p>Services could not be loaded. Check that the backend is running, then try again.</p>
        <button type="button" (click)="services.reload()">Try again</button>
      </div>
    } @else if (!services.value()?.length) {
      <p class="state-text">No services are monitored yet.</p>
    } @else {
      <div class="chips" role="group" aria-label="Filter by status">
        @for (f of filters; track f.value) {
          <button type="button" class="chip" [attr.aria-pressed]="filter() === f.value" (click)="filter.set(f.value)">
            {{ f.label }} <span class="count">{{ counts()[f.value] }}</span>
          </button>
        }
      </div>
      <div class="card table-card">
        <table class="table-cards">
          <caption class="visually-hidden">Monitored services</caption>
          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col">Owner</th>
              <th scope="col">Stage</th>
              <th scope="col">Status</th>
            </tr>
          </thead>
          <tbody>
            @for (service of visible(); track service.id) {
              <tr>
                <td data-label="Name" class="mono">{{ service.name }}</td>
                <td data-label="Owner">{{ service.ownerTeam }}</td>
                <td data-label="Stage">{{ service.stage }}</td>
                <td data-label="Status"><app-status-badge [value]="service.status" /></td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="state-text">No services with this status.</td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export class ServicesPage {
  private readonly api = inject(CatalogApi);

  protected readonly filters = FILTERS;
  protected readonly filter = signal<Filter>('ALL');
  protected readonly services = rxResource({ stream: () => this.api.services() });

  protected readonly counts = computed(() => {
    const all = this.services.value() ?? [];
    return {
      ALL: all.length,
      UP: all.filter((s) => s.status === 'UP').length,
      DEGRADED: all.filter((s) => s.status === 'DEGRADED').length,
      DOWN: all.filter((s) => s.status === 'DOWN').length,
    };
  });
  protected readonly visible = computed(() =>
    (this.services.value() ?? []).filter((s) => this.filter() === 'ALL' || s.status === this.filter()),
  );
}
