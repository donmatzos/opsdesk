import { TestBed } from '@angular/core/testing';
import { expectNoAxeViolations } from '../../testing/axe';
import { StatusBadge } from './status-badge';

function render(value: string) {
  const fixture = TestBed.createComponent(StatusBadge);
  fixture.componentRef.setInput('value', value);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('StatusBadge', () => {
  it('shows a status as a pill with text plus a hidden icon, never colour alone', async () => {
    const el = render('DOWN');
    expect(el.textContent?.trim()).toBe('DOWN');
    expect(el.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
    expect(el.classList.contains('pill')).toBe(true);
    expect(el.classList.contains('bad')).toBe(true);
    await expectNoAxeViolations(el);
  });

  it('shows a severity as a text-only tag, shaped differently from a state pill', () => {
    const sev = render('SEV2');
    expect(sev.textContent?.trim()).toBe('SEV2');
    expect(sev.querySelector('svg')).toBeNull();
    expect(sev.classList.contains('tag')).toBe(true);
    expect(sev.classList.contains('warn')).toBe(true);
    const state = render('MITIGATED');
    expect(state.classList.contains('pill')).toBe(true);
    expect(state.classList.contains('info')).toBe(true);
  });

  it('keeps severity and state visually distinct even in the same tone', () => {
    // SEV1 and OPEN are both "bad": one is a square text tag, the other a pill with an icon.
    expect(render('SEV1').className).not.toEqual(render('OPEN').className);
  });
});
