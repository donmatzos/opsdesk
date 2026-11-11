import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { expectNoAxeViolations } from '../../testing/axe';
import { GlossaryPage } from './glossary-page';

const MD = [
  '# Glossary',
  '',
  '## Helm',
  '',
  'Package manager for Kubernetes.',
  '',
  'Source: [Helm docs](https://helm.sh/docs/)',
  '',
  '## MTTR',
  '',
  'Mean time to resolve.',
  '',
  'Source: [Atlassian](https://www.atlassian.com/incident-management/kpis/common-metrics)',
  '',
  '## SLO',
  '',
  'A target for an SLI.',
  '',
  'Source: [SRE book](https://sre.google/sre-book/service-level-objectives/)',
].join('\n');

describe('GlossaryPage', () => {
  let http: HttpTestingController;

  async function render() {
    TestBed.configureTestingModule({ imports: [GlossaryPage], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(GlossaryPage);
    fixture.detectChanges();
    TestBed.tick();
    http.expectOne('guide/glossary.md').flush(MD);
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  function filter(fixture: { detectChanges(): void }, el: HTMLElement, text: string) {
    const input = el.querySelector('#glossary-filter') as HTMLInputElement;
    input.value = text;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('lists the terms with anchors and a labelled filter', async () => {
    const { el } = await render();
    expect(el.querySelector('label[for="glossary-filter"]')?.textContent).toContain('Filter terms');
    expect(Array.from(el.querySelectorAll('dt')).map((d) => d.textContent?.trim())).toEqual(['Helm', 'MTTR', 'SLO']);
    expect(el.querySelector('#mttr')).not.toBeNull();
    expect(el.querySelector('.count')?.textContent).toContain('Showing 3 of 3 terms');
  });

  it('filters case-insensitively and announces the result count', async () => {
    const { fixture, el } = await render();
    filter(fixture, el, 'slo');
    expect(Array.from(el.querySelectorAll('dt')).map((d) => d.textContent?.trim())).toEqual(['SLO']);
    expect(el.querySelector('[aria-live="polite"]')?.textContent).toContain('Showing 1 of 3 terms');
  });

  it('says so when nothing matches', async () => {
    const { fixture, el } = await render();
    filter(fixture, el, 'zzz');
    expect(el.textContent).toContain('No term matches "zzz"');
  });

  it('links each source in a new tab', async () => {
    const { el } = await render();
    const link = el.querySelector('.source a') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('https://helm.sh/docs/');
    expect(link.getAttribute('rel')).toBe('noopener noreferrer');
  });

  it('has no axe violations', async () => {
    const { el } = await render();
    await expectNoAxeViolations(el);
  });
});
