import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/** Temporary page for routes whose view is built in a later task of phase 7. */
@Component({
  selector: 'app-placeholder-page',
  template: `<h1>{{ title }}</h1><p>This view is coming soon.</p>`,
})
export class PlaceholderPage {
  protected readonly title = String(inject(ActivatedRoute).snapshot.title ?? '');
}
