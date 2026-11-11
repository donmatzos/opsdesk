import { TestBed } from '@angular/core/testing';
import { expectNoAxeViolations } from '../../testing/axe';
import { THEME_KEY } from './theme.service';
import { ThemeToggle } from './theme-toggle';

describe('ThemeToggle', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  function render() {
    const fixture = TestBed.createComponent(ThemeToggle);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('is a labelled group of three radio buttons with System selected', () => {
    const { el } = render();
    expect(el.querySelector('fieldset legend')?.textContent).toContain('Colour theme');
    const radios = Array.from(el.querySelectorAll('input[type="radio"]')) as HTMLInputElement[];
    expect(radios.map((r) => r.value)).toEqual(['system', 'light', 'dark']);
    expect(radios.map((r) => r.checked)).toEqual([true, false, false]);
    expect(radios.every((r) => r.closest('label')?.textContent?.trim())).toBe(true);
  });

  it('applies the chosen theme and remembers it', () => {
    const { fixture, el } = render();
    const dark = el.querySelector('input[value="dark"]') as HTMLInputElement;
    dark.click();
    fixture.detectChanges();
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(localStorage.getItem(THEME_KEY)).toBe('dark');
    expect(dark.checked).toBe(true);
  });

  it('has no axe violations', async () => {
    const { el } = render();
    await expectNoAxeViolations(el);
  });
});
