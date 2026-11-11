import { TestBed } from '@angular/core/testing';
import { Icon } from './icon';

describe('Icon', () => {
  it('draws the shapes of the icon (not stripped by the sanitizer)', async () => {
    const fixture = TestBed.createComponent(Icon);
    fixture.componentRef.setInput('name', 'circle-check');
    await fixture.whenStable();
    const svg = (fixture.nativeElement as HTMLElement).querySelector('svg') as SVGElement;
    expect(svg.getAttribute('aria-hidden')).toBe('true');
    expect(svg.querySelectorAll('circle').length).toBe(1);
    expect(svg.querySelector('path')?.getAttribute('d')).toBe('m9 12 2 2 4-4');
  });

  it('draws nothing for an unknown icon', async () => {
    const fixture = TestBed.createComponent(Icon);
    fixture.componentRef.setInput('name', 'nope');
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('svg > *').length).toBe(0);
  });
});
