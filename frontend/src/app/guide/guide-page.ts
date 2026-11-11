import { Component, ElementRef, afterRenderEffect, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { environment } from '../../environments/environment';
import { GuideContent } from './guide-content';
import { GUIDE_PAGES } from './guide-pages';
import { renderMarkdown, slugify } from './markdown';

/** One page of docs/guide rendered from Markdown. Route: /guide (overview) and /guide/:page. */
@Component({
  selector: 'app-guide-page',
  imports: [RouterLink],
  template: `
    @if (!current()) {
      <h1>Page not found</h1>
      <p>There is no guide page called "{{ page() }}". <a routerLink="/guide">Back to the project guide</a>.</p>
    } @else {
      @if (page()) {
        <nav aria-label="Breadcrumb" class="breadcrumb">
          <a routerLink="/guide">Project guide</a> / <span aria-current="page">{{ current()?.title }}</span>
        </nav>
      }
      @if (content.isLoading()) {
        <output class="state-text">Loading…</output>
      } @else if (content.error()) {
        <div class="alert" role="alert">
          <p>This page could not be loaded.</p>
          <button type="button" (click)="content.reload()">Try again</button>
        </div>
      } @else {
        <article class="prose" [innerHTML]="html()" (click)="onClick($event)"></article>
      }
    }
  `,
  styles: `.breadcrumb { margin-bottom: 1rem; font-size: 0.875rem; color: var(--muted); }`,
})
export class GuidePage {
  private readonly guide = inject(GuideContent);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /** Route parameter (withComponentInputBinding); undefined on /guide. */
  readonly page = input<string>();

  protected readonly current = computed(() => GUIDE_PAGES.find((p) => p.slug === (this.page() ?? '')));
  protected readonly content = rxResource({
    params: () => this.current()?.file,
    stream: ({ params }) => this.guide.load(params),
  });
  protected readonly html = computed(() =>
    renderMarkdown(this.content.value() ?? '', { repoUrl: environment.repoUrl, repoRef: environment.repoRef }),
  );

  constructor() {
    // The sanitizer strips id attributes from innerHTML, so heading ids are set on the rendered DOM afterwards.
    afterRenderEffect(() => {
      this.html();
      const root = this.host.nativeElement;
      root.querySelectorAll('.prose h1, .prose h2, .prose h3, .prose h4').forEach((h) => {
        h.id = slugify(h.textContent ?? '');
      });
      const fragment = this.route.snapshot.fragment;
      if (fragment) {
        root.querySelector(`[id="${CSS.escape(fragment)}"]`)?.scrollIntoView?.();
      }
    });
  }

  /** Links inside the rendered Markdown: internal ones go through the router, anchors scroll (a plain "#x" would resolve against <base href>). */
  protected onClick(event: MouseEvent): void {
    const link = (event.target as HTMLElement).closest('a');
    if (!link) {
      return;
    }
    if (link.classList.contains('internal-link')) {
      event.preventDefault();
      void this.router.navigateByUrl('/' + link.getAttribute('href'));
    } else if (link.classList.contains('anchor-link')) {
      event.preventDefault();
      const id = (link.getAttribute('href') ?? '').slice(1);
      this.host.nativeElement.querySelector(`[id="${CSS.escape(id)}"]`)?.scrollIntoView?.();
    }
  }
}
