import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

export type ThemePreference = 'system' | 'light' | 'dark';

export const THEME_KEY = 'opsdesk.theme';

/**
 * Colour scheme choice: system (follow the OS), light or dark. An explicit choice is stored in the browser and set as
 * data-theme on <html> (tokens in styles/_tokens.scss); the inline script in index.html applies it before first paint.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly root = inject(DOCUMENT).documentElement;
  readonly preference = signal<ThemePreference>(this.stored());

  constructor() {
    this.apply(this.preference());
  }

  set(preference: ThemePreference): void {
    this.preference.set(preference);
    this.apply(preference);
    try {
      if (preference === 'system') {
        localStorage.removeItem(THEME_KEY);
      } else {
        localStorage.setItem(THEME_KEY, preference);
      }
    } catch {
      // storage blocked (private mode): the choice still applies for this page view
    }
  }

  private apply(preference: ThemePreference): void {
    if (preference === 'system') {
      delete this.root.dataset['theme'];
    } else {
      this.root.dataset['theme'] = preference;
    }
  }

  private stored(): ThemePreference {
    try {
      const value = localStorage.getItem(THEME_KEY);
      return value === 'light' || value === 'dark' ? value : 'system';
    } catch {
      return 'system';
    }
  }
}
