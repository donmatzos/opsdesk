import { Component, inject } from '@angular/core';
import { Icon } from '../shared/icon';
import { ThemePreference, ThemeService } from './theme.service';

const OPTIONS: { value: ThemePreference; label: string; icon: string }[] = [
  { value: 'system', label: 'System', icon: 'monitor' },
  { value: 'light', label: 'Light', icon: 'sun' },
  { value: 'dark', label: 'Dark', icon: 'moon' },
];

/** Segmented control built from native radio buttons: arrow keys, labels and group semantics come for free. */
@Component({
  selector: 'app-theme-toggle',
  imports: [Icon],
  template: `
    <fieldset class="theme">
      <legend class="visually-hidden">Colour theme</legend>
      @for (option of options; track option.value) {
        <label>
          <input
            type="radio"
            name="theme"
            [value]="option.value"
            [checked]="theme.preference() === option.value"
            (change)="theme.set(option.value)"
          />
          <app-icon [name]="option.icon" />
          <span class="text">{{ option.label }}</span>
        </label>
      }
    </fieldset>
  `,
  styles: `
    .theme { display: inline-flex; margin: 0; padding: 2px; border: 1px solid var(--strong); border-radius: 999px; background: var(--surface); }
    label {
      position: relative;
      display: inline-flex;
      align-items: center;
      gap: 0.375rem;
      min-height: 2.25rem;
      padding: 0 0.75rem;
      border-radius: 999px;
      color: var(--muted);
      font-size: 0.875rem;
      cursor: pointer;
      transition: background-color var(--transition), color var(--transition);
    }
    label:has(input:checked) { background: var(--accent-soft); color: var(--accent-text); font-weight: 600; }
    label:has(input:focus-visible) { outline: 2px solid var(--focus); outline-offset: 2px; }
    input { position: absolute; inset: 0; opacity: 0; margin: 0; cursor: pointer; }
    @media (max-width: 639px) { .text { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); } label { padding: 0 0.625rem; } }
  `,
})
export class ThemeToggle {
  protected readonly theme = inject(ThemeService);
  protected readonly options = OPTIONS;
}
