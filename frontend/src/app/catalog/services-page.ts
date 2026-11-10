import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CatalogApi } from './catalog-api';

@Component({
  selector: 'app-services-page',
  template: `
    <h1>Services</h1>
    <table>
      <thead>
        <tr><th>Name</th><th>Owner</th><th>Stage</th><th>Status</th></tr>
      </thead>
      <tbody>
        @for (service of services(); track service.id) {
          <tr>
            <td>{{ service.name }}</td>
            <td>{{ service.ownerTeam }}</td>
            <td>{{ service.stage }}</td>
            <td><span class="status" [class]="service.status.toLowerCase()">{{ service.status }}</span></td>
          </tr>
        } @empty {
          <tr><td colspan="4">No services.</td></tr>
        }
      </tbody>
    </table>
  `,
  styles: `
    table { border-collapse: collapse; width: 100%; }
    th, td { text-align: left; padding: 0.5rem 0.75rem; border-bottom: 1px solid #e2e8f0; }
    .status { font-weight: 600; }
    .up { color: #15803d; }
    .degraded { color: #b45309; }
    .down { color: #b91c1c; }
  `,
})
export class ServicesPage {
  protected readonly services = toSignal(inject(CatalogApi).services(), { initialValue: [] });
}
