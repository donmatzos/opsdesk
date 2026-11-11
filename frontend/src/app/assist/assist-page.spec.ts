import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { expectNoAxeViolations } from '../../testing/axe';
import { AssistPage } from './assist-page';

const ANSWER = {
  answer:
    'MOCK answer (no language model was called). The most relevant runbook sections:\n[1] Pod not ready / Symptoms: - The readiness probe fails: `/readyz` is not 200. - `CrashLoopBackOff` in `oc get pods`.',
  sources: [
    { runbook: 'pod-not-ready', title: 'Pod not ready', section: 'Symptoms', path: 'docs/runbooks/pod-not-ready.md', score: 2.5 },
    { runbook: 'http-errors', title: 'HTTP errors (5xx)', section: 'First checks', path: 'docs/runbooks/http-errors.md', score: 1.1 },
  ],
  model: 'mock',
  mock: true,
};

describe('AssistPage', () => {
  let http: HttpTestingController;

  function render() {
    TestBed.configureTestingModule({ imports: [AssistPage], providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(AssistPage);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  function ask(el: HTMLElement, question: string) {
    const area = el.querySelector('textarea') as HTMLTextAreaElement;
    area.value = question;
    area.dispatchEvent(new Event('input'));
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
  }

  afterEach(() => http.verify());

  it('has a labelled question field', () => {
    const { el } = render();
    const area = el.querySelector('textarea') as HTMLTextAreaElement;
    expect(el.querySelector(`label[for="${area.id}"]`)?.textContent).toContain('Your question');
    expect(area.getAttribute('maxlength')).toBe('500');
  });

  it('sends the question, shows a formatted answer, one mock badge and source cards linking to the repo', async () => {
    const { fixture, el } = render();
    ask(el, 'pod not ready');
    http.expectOne('/api/assist').flush(ANSWER);
    await fixture.whenStable();
    fixture.detectChanges();
    const live = el.querySelector('[aria-live="polite"]') as HTMLElement;
    expect(live.querySelector('.answer-item code')?.textContent).toBe('/readyz');
    expect(live.querySelector('.card-body')?.textContent).not.toContain('no language model was called');
    expect(el.textContent?.match(/Mock answer/g)?.length).toBe(1);
    const links = Array.from(el.querySelectorAll('.source-card a')) as HTMLAnchorElement[];
    expect(links.length).toBe(2);
    expect(links[0].getAttribute('href')).toBe('https://github.com/donmatzos/opsdesk/blob/main/docs/runbooks/pod-not-ready.md');
    expect(links[0].textContent).toContain('Pod not ready');
    expect(links[0].textContent).toContain('Symptoms');
  });

  it('offers example questions that ask right away', () => {
    const { el } = render();
    const chips = Array.from(el.querySelectorAll('.example-chips button')) as HTMLButtonElement[];
    expect(chips.length).toBeGreaterThanOrEqual(3);
    chips[0].click();
    const req = http.expectOne('/api/assist');
    expect(req.request.body.question).toBe(chips[0].textContent?.trim());
    req.flush(ANSWER);
  });

  it('disables the button while the request is pending', () => {
    const { fixture, el } = render();
    ask(el, 'x');
    fixture.detectChanges();
    expect((el.querySelector('button[type="submit"]') as HTMLButtonElement).disabled).toBe(true);
    http.expectOne('/api/assist').flush(ANSWER);
  });

  it('does not send an empty question and shows a field error', () => {
    const { fixture, el } = render();
    ask(el, '   ');
    fixture.detectChanges();
    http.expectNone('/api/assist');
    expect(el.querySelector('.field-error')?.textContent).toContain('Type a question');
  });

  it('explains when the assistant is not available (404 or 503)', async () => {
    const { fixture, el } = render();
    ask(el, 'x');
    http.expectOne('/api/assist').flush({}, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('not available in this deployment');
  });

  it('has no axe violations', async () => {
    const { fixture, el } = render();
    ask(el, 'x');
    http.expectOne('/api/assist').flush(ANSWER);
    await fixture.whenStable();
    fixture.detectChanges();
    await expectNoAxeViolations(el);
  });
});
