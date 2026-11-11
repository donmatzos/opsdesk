import { Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { StatusBadge } from '../shared/status-badge';
import { CatalogApi } from './catalog-api';

const ORDER = ['UP', 'DEGRADED', 'DOWN'] as const;

@Component({
  selector: 'app-services-page',
  imports: [StatusBadge],
  template: `
    <h1>Services</h1>
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
      <p class="summary">{{ summary() }}</p>
      <section class="table-scroll" tabindex="0" aria-label="Services table">
        <table>
          <caption>Monitored services</caption>
          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col">Owner</th>
              <th scope="col">Stage</th>
              <th scope="col">Status</th>
            </tr>
          </thead>
          <tbody>
            @for (service of services.value(); track service.id) {
              <tr>
                <td class="mono">{{ service.name }}</td>
                <td>{{ service.ownerTeam }}</td>
                <td>{{ service.stage }}</td>
                <td><app-status-badge [value]="service.status" /></td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    }
  `,
})
export class ServicesPage {
  private readonly api = inject(CatalogApi);

  protected readonly services = rxResource({ stream: () => this.api.services() });

  /** "4 services: 2 UP, 1 DEGRADED, 1 DOWN" as text, so the overview does not depend on colour or icons. */
  protected readonly summary = computed(() => {
    const all = this.services.value() ?? [];
    const counts = ORDER.map((s) => [s, all.filter((x) => x.status === s).length] as const).filter(([, n]) => n > 0);
    const parts = counts.map(([s, n]) => n + ' ' + s);
    return `${all.length} ${all.length === 1 ? 'service' : 'services'}: ${parts.join(', ')}`;
  });
}
