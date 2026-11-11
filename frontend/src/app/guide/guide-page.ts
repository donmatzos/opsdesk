import { Component, ElementRef, afterRenderEffect, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { environment } from '../../environments/environment';
import { GuideContent } from './guide-content';
import { GUIDE_PAGES } from './guide-pages';
import { extractToc, renderMarkdown, slugify } from './markdown';

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
        <div class="guide-layout">
          <article class="prose" [innerHTML]="html()" (click)="onClick($event)"></article>
          @if (toc().length > 1) {
            <nav class="toc" aria-label="On this page">
              <h2>On this page</h2>
              <ul>
                @for (h of toc(); track h.id) {
                  <li [class.sub]="h.level === 3"><a [routerLink]="[]" [fragment]="h.id">{{ h.text }}</a></li>
                }
              </ul>
            </nav>
          }
        </div>
      }
    }
  `,
  styles: `
    .breadcrumb { margin-bottom: 1rem; font-size: 0.875rem; color: var(--muted); }
    .guide-layout { display: grid; gap: 3rem; grid-template-columns: minmax(0, var(--prose-width)); }
    .toc { display: none; }
    @media (min-width: 1200px) {
      .guide-layout { grid-template-columns: minmax(0, var(--prose-width)) 15rem; }
      .toc { display: block; position: sticky; top: calc(var(--header-height) + 1.5rem); align-self: start; max-height: calc(100dvh - var(--header-height) - 3rem); overflow-y: auto; }
    }
    .toc h2 { margin: 0 0 0.5rem; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.08em; color: var(--muted); }
    .toc ul { list-style: none; margin: 0; padding: 0; border-left: 2px solid var(--border); }
    .toc li a { display: block; padding: 0.25rem 0.75rem; color: var(--muted); font-size: 0.875rem; text-decoration: none; }
    .toc li a:hover { color: var(--text); }
    .toc li.sub a { padding-left: 1.5rem; }
  `,
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
  protected readonly toc = computed(() => extractToc(this.content.value() ?? ''));
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
