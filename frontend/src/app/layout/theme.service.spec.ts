import { TestBed } from '@angular/core/testing';
import { THEME_KEY, ThemeService } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it('follows the system by default (no data-theme attribute, nothing stored)', () => {
    const theme = TestBed.inject(ThemeService);
    expect(theme.preference()).toBe('system');
    expect(document.documentElement.hasAttribute('data-theme')).toBe(false);
    expect(localStorage.getItem(THEME_KEY)).toBeNull();
  });

  it('applies and remembers an explicit choice', () => {
    const theme = TestBed.inject(ThemeService);
    theme.set('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(localStorage.getItem(THEME_KEY)).toBe('dark');
    theme.set('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('going back to system removes the attribute and the stored value', () => {
    const theme = TestBed.inject(ThemeService);
    theme.set('dark');
    theme.set('system');
    expect(document.documentElement.hasAttribute('data-theme')).toBe(false);
    expect(localStorage.getItem(THEME_KEY)).toBeNull();
  });

  it('restores a stored choice and ignores junk', () => {
    localStorage.setItem(THEME_KEY, 'light');
    expect(TestBed.inject(ThemeService).preference()).toBe('light');
    TestBed.resetTestingModule();
    localStorage.setItem(THEME_KEY, 'purple');
    expect(TestBed.inject(ThemeService).preference()).toBe('system');
  });
});
