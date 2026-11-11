import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { environment } from '../../environments/environment';
import { GuideContent } from './guide-content';
import { GLOSSARY_FILE } from './guide-pages';
import { filterGlossary, parseGlossary } from './glossary';

@Component({
  selector: 'app-glossary-page',
  imports: [RouterLink],
  template: `
    <h1>Glossary</h1>
    <p class="intro">Terms used in this project, each with a link to an authoritative source.</p>

    @if (content.isLoading()) {
      <output class="state-text">Loading glossary…</output>
    } @else if (content.error()) {
      <div class="alert" role="alert">
        <p>The glossary could not be loaded.</p>
        <button type="button" (click)="content.reload()">Try again</button>
      </div>
    } @else {
      <div class="field">
        <label for="glossary-filter">Filter terms</label>
        <input
          #filterInput
          id="glossary-filter"
          type="search"
          autocomplete="off"
          [value]="query()"
          (input)="query.set(filterInput.value)"
        />
      </div>
      <p aria-live="polite" class="count">Showing {{ shown().length }} of {{ entries().length }} terms</p>

      @if (!query()) {
        <nav aria-label="Jump to letter" class="letters">
          @for (letter of letters(); track letter) {
            <a [routerLink]="[]" [fragment]="'letter-' + letter">{{ letter }}</a>
          }
        </nav>
      }

      <dl class="glossary">
        @for (entry of shown(); track entry.id) {
          <div class="card entry">
            <dt [id]="entry.id">
              <span [id]="'letter-' + entry.term[0].toUpperCase()" class="letter-anchor"></span>{{ entry.term }}
            </dt>
            <dd>
              <div [innerHTML]="entry.definitionHtml" (click)="onClick($event)"></div>
              <p class="links">
                <span class="source">
                  Source:
                  <a [href]="entry.source.url" target="_blank" rel="noopener noreferrer"
                    >{{ entry.source.label }}<span class="visually-hidden"> (opens in a new tab)</span></a
                  >
                </span>
                @if (entry.repoHtml) {
                  <span class="repo" (click)="onClick($event)">In this repo: <span [innerHTML]="entry.repoHtml"></span></span>
                }
              </p>
            </dd>
          </div>
        } @empty {
          <p class="state-text">No term matches "{{ query() }}".</p>
        }
      </dl>
    }
  `,
  styles: `
    .intro, .count { color: var(--muted); }
    .letters {
      position: sticky;
      top: var(--header-height);
      z-index: 5;
      display: flex;
      flex-wrap: wrap;
      gap: 0.25rem;
      margin: 0 0 1rem;
      padding: 0.5rem 0;
      background: var(--bg);
    }
    .letters a { min-width: 2.25rem; min-height: 2.25rem; display: inline-flex; align-items: center; justify-content: center; border-radius: var(--radius); color: var(--accent-text); font-weight: 600; text-decoration: none; }
    .letters a:hover { background: var(--accent-soft); }
    .glossary { margin: 0; display: grid; gap: 1rem; grid-template-columns: repeat(auto-fill, minmax(min(100%, 24rem), 1fr)); }
    .entry { padding: 1rem 1.25rem; scroll-margin-top: 6rem; }
    dt { font-weight: 600; font-size: 1.125rem; scroll-margin-top: 6rem; }
    dd { margin: 0.375rem 0 0; }
    dd p, dd div { margin: 0.25rem 0; }
    .links { display: flex; flex-wrap: wrap; gap: 0.25rem 1.25rem; margin-top: 0.75rem; font-size: 0.875rem; color: var(--muted); }
  `,
})
export class GlossaryPage {
  private readonly guide = inject(GuideContent);
  private readonly router = inject(Router);

  protected readonly content = rxResource({ stream: () => this.guide.load(GLOSSARY_FILE) });
  protected readonly entries = computed(() => {
    const md = this.content.value();
    return md ? parseGlossary(md, { repoUrl: environment.repoUrl, repoRef: environment.repoRef }) : [];
  });
  protected readonly query = signal('');
  protected readonly shown = computed(() => filterGlossary(this.entries(), this.query()));
  protected readonly letters = computed(() => [...new Set(this.entries().map((e) => e.term[0].toUpperCase()))].sort((a, b) => a.localeCompare(b)));

  /** "In this repo" links to guide pages go through the router; repo links are normal external links. */
  protected onClick(event: MouseEvent): void {
    const link = (event.target as HTMLElement).closest('a.internal-link');
    if (link) {
      event.preventDefault();
      void this.router.navigateByUrl('/' + link.getAttribute('href'));
    }
  }
}
