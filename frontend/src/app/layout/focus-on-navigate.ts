import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';

/** After a route change focus moves to <main> so keyboard and screen reader users start at the new content (not on first load). */
@Injectable({ providedIn: 'root' })
export class FocusOnNavigate {
  private readonly doc = inject(DOCUMENT);
  private navigations = 0;

  constructor() {
    inject(Router)
      .events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => {
        if (++this.navigations > 1) {
          this.focusMain();
        }
      });
  }

  focusMain(): void {
    this.doc.getElementById('main')?.focus();
  }
}
