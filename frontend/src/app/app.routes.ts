import { Routes } from '@angular/router';
import { ServicesPage } from './catalog/services-page';
import { IncidentsPage } from './incidents/incidents-page';

// Every route here must also be forwarded to index.html by the backend's SpaForwardController.
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'services' },
  { path: 'services', component: ServicesPage },
  { path: 'incidents', component: IncidentsPage },
  { path: 'incidents/:id', component: IncidentsPage },
];
