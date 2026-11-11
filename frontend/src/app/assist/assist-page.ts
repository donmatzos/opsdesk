import { Component, signal } from '@angular/core';
import { inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { AssistAnswer, AssistApi, MAX_QUESTION_LENGTH } from './assist-api';

@Component({
  selector: 'app-assist-page',
  template: `
    <h1>Ask the runbooks</h1>
    <p>
      Ask an operations question. The answer is built from the runbooks in the repository, and every answer lists the
      sections it came from.
    </p>

    <form (submit)="submit($event)" novalidate>
      <div class="field">
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
        <span id="question-hint" class="hint">For example: pod not ready after deploy. Up to {{ max }} characters.</span>
        @if (fieldError()) {
          <span id="question-error" class="field-error">{{ fieldError() }}</span>
        }
      </div>
      <button type="submit" class="primary" [disabled]="pending()">{{ pending() ? 'Searching…' : 'Ask' }}</button>
    </form>

    @if (failure()) {
      <div class="alert" role="alert">{{ failure() }}</div>
    }

    <div aria-live="polite">
      @if (result(); as r) {
        <h2>Answer</h2>
        @if (r.mock) {
          <p class="badge">Mock answer: no language model was called</p>
        }
        <p class="answer">{{ r.answer }}</p>
        @if (r.sources.length) {
          <h2>Sources</h2>
          <ul>
            @for (source of r.sources; track source.path + source.section) {
              <li>
                <a [href]="sourceUrl(source.path)" target="_blank" rel="noopener noreferrer"
                  >{{ source.title }} / {{ source.section }}<span class="visually-hidden"> (opens in a new tab)</span></a
                >
              </li>
            }
          </ul>
        }
      }
    </div>
  `,
  styles: `
    textarea { resize: vertical; }
    .answer { white-space: pre-wrap; max-width: var(--prose-width); }
    .badge { display: inline-block; border: 1px solid var(--warning); color: var(--warning); border-radius: var(--radius); padding: 0.125rem 0.5rem; font-size: 0.875rem; font-weight: 600; }
  `,
})
export class AssistPage {
  private readonly api = inject(AssistApi);

  protected readonly max = MAX_QUESTION_LENGTH;
  protected readonly question = signal('');
  protected readonly fieldError = signal('');
  protected readonly failure = signal('');
  protected readonly pending = signal(false);
  protected readonly result = signal<AssistAnswer | null>(null);

  protected sourceUrl(path: string): string {
    return `${environment.repoUrl}/blob/${environment.repoRef}/${path}`;
  }

  protected submit(event: Event): void {
    event.preventDefault();
    this.failure.set('');
    const text = this.question().trim();
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
