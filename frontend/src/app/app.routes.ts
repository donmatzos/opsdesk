import { Routes } from '@angular/router';
import { environment } from '../environments/environment';
import { ServicesPage } from './catalog/services-page';
import { AssistPage } from './assist/assist-page';
import { GlossaryPage } from './guide/glossary-page';
import { GuidePage } from './guide/guide-page';
import { GUIDE_PAGES } from './guide/guide-pages';
import { IncidentDetailPage } from './incidents/incident-detail-page';
import { IncidentsPage } from './incidents/incidents-page';
import { OverviewPage } from './overview/overview-page';
import { NotFoundPage } from './shared/not-found-page';

// Every route here must also be forwarded to index.html by the backend's SpaForwardController (and be in SpaRoutingTest).
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: environment.homeRoute },
  { path: 'overview', component: OverviewPage, title: 'Overview' },
  { path: 'services', component: ServicesPage, title: 'Services' },
  { path: 'incidents', component: IncidentsPage, title: 'Incidents' },
  { path: 'incidents/:id', component: IncidentDetailPage, title: 'Incident' },
  { path: 'assist', component: AssistPage, title: 'Ask the runbooks' },
  { path: 'guide', component: GuidePage, title: 'Project guide' },
  { path: 'guide/glossary', component: GlossaryPage, title: 'Glossary' },
  {
    path: 'guide/:page',
    component: GuidePage,
    title: (route) => GUIDE_PAGES.find((p) => p.slug === route.params['page'])?.title ?? 'Page not found',
  },
  { path: '**', component: NotFoundPage, title: 'Page not found' },
];
