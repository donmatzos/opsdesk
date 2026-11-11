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
      <p class="state-text" role="status">Loading glossary…</p>
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
          <dt [id]="entry.id">
            <span [id]="'letter-' + entry.term[0].toUpperCase()" class="letter-anchor"></span>{{ entry.term }}
          </dt>
          <dd>
            <div [innerHTML]="entry.definitionHtml" (click)="onClick($event)"></div>
            <p class="source">
              Source:
              <a [href]="entry.source.url" target="_blank" rel="noopener noreferrer"
                >{{ entry.source.label }}<span class="visually-hidden"> (opens in a new tab)</span></a
              >
            </p>
            @if (entry.repoHtml) {
              <div class="repo" (click)="onClick($event)">In this repo: <span [innerHTML]="entry.repoHtml"></span></div>
            }
          </dd>
        } @empty {
          <p class="state-text">No term matches "{{ query() }}".</p>
        }
      </dl>
    }
  `,
  styles: `
    .intro, .count { color: var(--muted); }
    .letters { display: flex; flex-wrap: wrap; gap: 0.25rem; margin-bottom: 1.5rem; }
    .letters a { min-width: 2.75rem; min-height: 2.75rem; display: inline-flex; align-items: center; justify-content: center; border: 1px solid var(--border); border-radius: var(--radius); }
    dt { font-weight: 600; font-size: 1.125rem; margin-top: 1.5rem; scroll-margin-top: 1rem; }
    dd { margin: 0.25rem 0 0; max-width: var(--prose-width); }
    dd p, dd div { margin: 0.25rem 0; }
    .source, .repo { font-size: 0.875rem; color: var(--muted); }
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
  protected readonly letters = computed(() => [...new Set(this.entries().map((e) => e.term[0].toUpperCase()))].sort());

  /** "In this repo" links to guide pages go through the router; repo links are normal external links. */
  protected onClick(event: MouseEvent): void {
    const link = (event.target as HTMLElement).closest('a.internal-link');
    if (link) {
      event.preventDefault();
      void this.router.navigateByUrl('/' + link.getAttribute('href'));
    }
  }
}
