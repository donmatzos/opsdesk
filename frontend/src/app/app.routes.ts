import { Routes } from '@angular/router';
import { environment } from '../environments/environment';
import { ServicesPage } from './catalog/services-page';
import { AssistPage } from './assist/assist-page';
import { GuidePage } from './guide/guide-page';
import { GUIDE_PAGES } from './guide/guide-pages';
import { IncidentDetailPage } from './incidents/incident-detail-page';
import { IncidentsPage } from './incidents/incidents-page';
import { NotFoundPage } from './shared/not-found-page';
import { PlaceholderPage } from './shared/placeholder-page';

// Every route here must also be forwarded to index.html by the backend's SpaForwardController (and be in SpaRoutingTest).
// Guide and assist views are placeholders until their tasks (phase 7).
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: environment.homeRoute },
  { path: 'services', component: ServicesPage, title: 'Services' },
  { path: 'incidents', component: IncidentsPage, title: 'Incidents' },
  { path: 'incidents/:id', component: IncidentDetailPage, title: 'Incident' },
  { path: 'assist', component: AssistPage, title: 'Ask the runbooks' },
  { path: 'guide', component: GuidePage, title: 'Project guide' },
  { path: 'guide/glossary', component: PlaceholderPage, title: 'Glossary' },
  {
    path: 'guide/:page',
    component: GuidePage,
    title: (route) => GUIDE_PAGES.find((p) => p.slug === route.params['page'])?.title ?? 'Page not found',
  },
  { path: '**', component: NotFoundPage, title: 'Page not found' },
];
