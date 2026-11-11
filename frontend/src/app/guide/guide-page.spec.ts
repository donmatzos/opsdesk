import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { expectNoAxeViolations } from '../../testing/axe';
import { GuidePage } from './guide-page';

const MD = [
  '# Architecture',
  '',
  'Intro with [glossary](glossary.md#mttr) and [values](../../deploy/helm/opsdesk/values.yaml).',
  '',
  '## Error budget',
  '',
  'Text <script>window.pwned = true</script> end.',
].join('\n');

describe('GuidePage', () => {
  let http: HttpTestingController;

  function render(page?: string) {
    TestBed.configureTestingModule({ imports: [GuidePage], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(GuidePage);
    if (page !== undefined) {
      fixture.componentRef.setInput('page', page);
    }
    fixture.detectChanges();
    TestBed.tick();
    return fixture;
  }

  async function load(fixture: ReturnType<typeof render>, file: string, body: string) {
    http.expectOne(`guide/${file}`).flush(body);
    await fixture.whenStable();
    fixture.detectChanges();
    TestBed.tick();
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => http.verify());

  it('renders the Markdown, sets heading ids and never lets a script through', async () => {
    const el = await load(render('architecture'), 'architecture.md', MD);
    expect(el.querySelector('h1')?.textContent).toBe('Architecture');
    expect(el.querySelector('h2')?.id).toBe('error-budget');
    expect(el.querySelector('script')).toBeNull();
    expect((window as unknown as { pwned?: boolean }).pwned).toBeUndefined();
  });

  it('shows the overview at /guide without breadcrumbs, and a breadcrumb on sub pages', async () => {
    const overview = await load(render(), 'index.md', '# Project guide');
    expect(overview.querySelector('.breadcrumb')).toBeNull();
    TestBed.resetTestingModule();
    const sub = await load(render('design'), 'design.md', '# Design');
    expect(sub.querySelector('.breadcrumb')?.textContent).toContain('Design and accessibility');
  });

  it('navigates internal links through the router without a page reload', async () => {
    const fixture = render('architecture');
    const el = await load(fixture, 'architecture.md', MD);
    const spy = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const link = el.querySelector('a.internal-link') as HTMLAnchorElement;
    const event = new MouseEvent('click', { bubbles: true, cancelable: true });
    link.dispatchEvent(event);
    expect(event.defaultPrevented).toBe(true);
    expect(spy).toHaveBeenCalledWith('/guide/glossary#mttr');
  });

  it('says so for an unknown page and loads nothing', () => {
    const el = render('nope').nativeElement as HTMLElement;
    expect(el.textContent).toContain('Page not found');
    http.expectNone(() => true);
  });

  it('has no axe violations', async () => {
    const el = await load(render('architecture'), 'architecture.md', MD);
    await expectNoAxeViolations(el);
  });
});
