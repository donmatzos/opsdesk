import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { environment } from '../../environments/environment';
import { Icon } from '../shared/icon';
import { AssistAnswer, AssistApi, AssistSource, MAX_QUESTION_LENGTH } from './assist-api';
import { formatAnswer } from './format-answer';

const EXAMPLES = ['Pod not ready after deploy', 'Database connection refused', 'Rising 5xx error rate', 'How do I rotate the MCP token?'];

@Component({
  selector: 'app-assist-page',
  imports: [Icon],
  template: `
    <header class="page-header">
      <h1>Ask the runbooks</h1>
      <p class="lede">
        Ask an operations question. The answer is built from the runbooks in the repository, and every answer lists the
        sections it came from.
      </p>
    </header>

    <section class="card disclosure-panel" aria-labelledby="ask-heading">
      <h2 id="ask-heading" class="visually-hidden">Your question</h2>
      <form (submit)="submit($event)" novalidate>
        <div class="field wide">
          <label for="question">Your question</label>
          <textarea
            #questionInput
            id="question"
            rows="3"
            [attr.maxlength]="max"
            [value]="question()"
            (input)="question.set(questionInput.value)"
            [attr.aria-invalid]="fieldError() ? 'true' : null"
            [attr.aria-describedby]="fieldError() ? 'question-error question-hint' : 'question-hint'"
          ></textarea>
          <span id="question-hint" class="hint">Up to {{ max }} characters.</span>
          @if (fieldError()) {
            <span id="question-error" class="field-error">{{ fieldError() }}</span>
          }
        </div>
        <button type="submit" class="primary" [disabled]="pending()">{{ pending() ? 'Searching…' : 'Ask' }}</button>
      </form>
      <p class="meta">Or try an example:</p>
      <div class="example-chips">
        @for (example of examples; track example) {
          <button type="button" class="chip" [disabled]="pending()" (click)="ask(example)">{{ example }}</button>
        }
      </div>
    </section>

    @if (failure()) {
      <div class="alert" role="alert">{{ failure() }}</div>
    }

    <div aria-live="polite" class="results">
      @if (result(); as r) {
        <section class="card answer-card" aria-labelledby="answer-heading">
          <div class="card-head">
            <h2 id="answer-heading">Answer</h2>
            @if (r.mock) {
              <span class="mock-badge">Mock answer: no language model was called</span>
            }
          </div>
          <div class="card-body">
            @if (formatted().items.length) {
              <ol class="answer-items">
                @for (item of formatted().items; track item.n) {
                  <li class="answer-item">
                    <span class="n" aria-hidden="true">{{ item.n }}</span>
                    <div>
                      <strong>{{ item.label }}</strong>
                      <div [innerHTML]="item.html"></div>
                    </div>
                  </li>
                }
              </ol>
            } @else {
              <div class="prose" [innerHTML]="formatted().html"></div>
            }
          </div>
        </section>

        @if (r.sources.length) {
          <section aria-labelledby="sources-heading">
            <h2 id="sources-heading" class="section">Sources</h2>
            <ul class="source-grid">
              @for (source of r.sources; track source.path + source.section) {
                <li class="card source-card">
                  <a [href]="sourceUrl(source.path)" target="_blank" rel="noopener noreferrer">
                    <span class="title">{{ source.title }} / {{ source.section }}</span>
                    <span class="meta mono">{{ source.path }}</span>
                    <span class="visually-hidden"> (opens in a new tab)</span>
                    <app-icon name="external-link" />
                  </a>
                </li>
              }
            </ul>
          </section>
        }
      } @else if (!pending() && !failure()) {
        <p class="meta empty">Answers appear here, with the runbook sections they came from.</p>
      }
    </div>
  `,
  styles: `
    textarea { resize: vertical; }
    .results { display: grid; gap: 1.5rem; margin-top: 1.5rem; }
    .mock-badge { border: 1px solid var(--warning); background: var(--warning-soft); color: var(--warning); border-radius: 999px; padding: 0.25rem 0.75rem; font-size: 0.8125rem; font-weight: 600; }
    .answer-items { list-style: none; margin: 0; padding: 0; display: grid; gap: 1rem; }
    .answer-item { display: grid; grid-template-columns: 1.75rem minmax(0, 1fr); gap: 0.75rem; }
    .answer-item .n { display: inline-flex; align-items: center; justify-content: center; width: 1.75rem; height: 1.75rem; border-radius: 999px; background: var(--accent-soft); color: var(--accent-text); font-weight: 600; font-size: 0.875rem; }
    .answer-item :is(p) { margin: 0.25rem 0 0; max-width: var(--prose-width); }
    h2.section { font-size: 1.125rem; margin: 0 0 0.75rem; }
    .source-grid { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr)); gap: 1rem; }
    .source-card a { display: grid; grid-template-columns: 1fr auto; gap: 0.25rem 0.5rem; padding: 0.875rem 1rem; color: var(--text); text-decoration: none; border-radius: var(--radius-lg); }
    .source-card a:hover { background: var(--surface-2); }
    .source-card .title { font-weight: 600; }
    .source-card .meta { grid-column: 1; }
    .source-card app-icon { grid-column: 2; grid-row: 1 / span 2; align-self: center; color: var(--muted); }
    .empty { padding: 1rem 0; }
  `,
})
export class AssistPage {
  private readonly api = inject(AssistApi);

  protected readonly max = MAX_QUESTION_LENGTH;
  protected readonly examples = EXAMPLES;
  protected readonly question = signal('');
  protected readonly fieldError = signal('');
  protected readonly failure = signal('');
  protected readonly pending = signal(false);
  protected readonly result = signal<AssistAnswer | null>(null);
  protected readonly formatted = computed(() => {
    const r = this.result();
    return r
      ? formatAnswer(r.answer, r.mock, { repoUrl: environment.repoUrl, repoRef: environment.repoRef })
      : { items: [], html: '' };
  });

  protected sourceUrl(path: AssistSource['path']): string {
    return `${environment.repoUrl}/blob/${environment.repoRef}/${path}`;
  }

  protected ask(example: string): void {
    this.question.set(example);
    this.send(example);
  }

  protected submit(event: Event): void {
    event.preventDefault();
    this.send(this.question().trim());
  }

  private send(text: string): void {
    this.failure.set('');
    if (!text) {
      this.fieldError.set('Type a question first.');
      document.getElementById('question')?.focus();
      return;
    }
    this.fieldError.set('');
    this.pending.set(true);
    this.result.set(null);
    this.api.ask(text).subscribe({
      next: (answer) => {
        this.pending.set(false);
        this.result.set(answer);
      },
      error: (e: unknown) => {
        this.pending.set(false);
        const status = e instanceof HttpErrorResponse ? e.status : 0;
        this.failure.set(
          status === 404 || status === 503
            ? 'The runbook assistant is not available in this deployment.'
            : 'The question could not be answered. Try again later.',
        );
      },
    });
  }
}
