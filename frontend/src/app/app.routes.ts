import { Routes } from '@angular/router';
import { environment } from '../environments/environment';
import { ServicesPage } from './catalog/services-page';
import { IncidentsPage } from './incidents/incidents-page';
import { NotFoundPage } from './shared/not-found-page';
import { PlaceholderPage } from './shared/placeholder-page';

// Every route here must also be forwarded to index.html by the backend's SpaForwardController (and be in SpaRoutingTest).
// Guide and assist views are placeholders until their tasks (phase 7).
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: environment.homeRoute },
  { path: 'services', component: ServicesPage, title: 'Services' },
  { path: 'incidents', component: IncidentsPage, title: 'Incidents' },
  { path: 'incidents/:id', component: IncidentsPage, title: 'Incident' },
  { path: 'assist', component: PlaceholderPage, title: 'Ask the runbooks' },
  { path: 'guide', component: PlaceholderPage, title: 'Project guide' },
  { path: 'guide/architecture', component: PlaceholderPage, title: 'Architecture' },
  { path: 'guide/skills', component: PlaceholderPage, title: 'What this shows' },
  { path: 'guide/design', component: PlaceholderPage, title: 'Design and accessibility' },
  { path: 'guide/glossary', component: PlaceholderPage, title: 'Glossary' },
  { path: '**', component: NotFoundPage, title: 'Page not found' },
];
