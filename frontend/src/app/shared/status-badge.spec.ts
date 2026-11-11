import { TestBed } from '@angular/core/testing';
import { expectNoAxeViolations } from '../../testing/axe';
import { StatusBadge } from './status-badge';

describe('StatusBadge', () => {
  it('shows the status as text plus a hidden icon, never colour alone', async () => {
    const fixture = TestBed.createComponent(StatusBadge);
    fixture.componentRef.setInput('value', 'DOWN');
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent?.trim()).toBe('DOWN');
    expect(el.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
    expect(el.classList.contains('bad')).toBe(true);
    await expectNoAxeViolations(el);
  });
});
