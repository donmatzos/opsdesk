import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { expectNoAxeViolations } from '../../testing/axe';
import { Shell } from './shell';

@Component({ template: `<h1>Page</h1>` })
class Dummy {}

const routes = ['services', 'incidents', 'assist', 'guide', 'guide/architecture', 'guide/skills', 'guide/design', 'guide/glossary'].map(
  (path) => ({ path, component: Dummy }),
);

describe('Shell', () => {
  async function render() {
    TestBed.configureTestingModule({ imports: [Shell], providers: [provideRouter(routes)] });
    const fixture = TestBed.createComponent(Shell);
    await TestBed.inject(Router).navigateByUrl('/incidents');
    fixture.detectChanges();
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('groups the navigation into Dashboard and Project guide', async () => {
    const { el } = await render();
    const groups = Array.from(el.querySelectorAll('nav h2')).map((h) => h.textContent?.trim());
    expect(groups).toEqual(['Dashboard', 'Project guide']);
    const dashboard = Array.from(el.querySelectorAll('nav section:first-of-type a')).map((a) => a.textContent?.trim());
    expect(dashboard).toEqual(['Services', 'Incidents', 'Ask the runbooks']);
    const guide = Array.from(el.querySelectorAll('nav section:last-of-type a')).map((a) => a.textContent?.trim());
    expect(guide).toEqual(['Overview', 'Architecture', 'What this shows', 'Design and accessibility', 'Glossary']);
  });

  it('marks the active link with aria-current', async () => {
    const { el } = await render();
    const current = el.querySelectorAll('nav a[aria-current="page"]');
    expect(current.length).toBe(1);
    expect(current[0].textContent).toContain('Incidents');
  });

  it('starts with a skip link to main', async () => {
    const { el } = await render();
    const first = el.querySelector('a, button') as HTMLAnchorElement;
    expect(first.textContent).toContain('Skip to main content');
    expect(first.getAttribute('href')).toBe('#main');
  });

  it('flips aria-expanded when the menu button is used', async () => {
    const { fixture, el } = await render();
    const button = el.querySelector('button.menu-toggle') as HTMLButtonElement;
    expect(button.getAttribute('aria-expanded')).toBe('false');
    expect(button.getAttribute('aria-controls')).toBe('site-nav');
    button.click();
    fixture.detectChanges();
    expect(button.getAttribute('aria-expanded')).toBe('true');
  });

  it('has a theme toggle and an icon-only GitHub link with an accessible name', async () => {
    const { el } = await render();
    expect(el.querySelector('header app-theme-toggle')).not.toBeNull();
    const repo = el.querySelector('header a.repo') as HTMLAnchorElement;
    expect(repo.getAttribute('aria-label')).toBe('Source on GitHub (opens in a new tab)');
    expect(repo.getAttribute('rel')).toBe('noopener noreferrer');
  });

  it('keeps the showcase note in the sidebar, not in a page footer', async () => {
    const { el } = await render();
    expect(el.querySelector('nav .sidebar-note')?.textContent).toContain('Cloud parts are mocked');
    expect(el.querySelector('footer')).toBeNull();
  });

  it('opens the phone drawer: main becomes inert, focus moves into it, Escape closes and restores focus', async () => {
    const { fixture, el } = await render();
    const toggle = el.querySelector('button.menu-toggle') as HTMLButtonElement;
    toggle.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(el.querySelector('main')?.hasAttribute('inert')).toBe(true);
    expect(document.activeElement?.closest('nav')).not.toBeNull();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    expect(el.querySelector('main')?.hasAttribute('inert')).toBe(false);
    expect(document.activeElement).toBe(toggle);
  });

  it('closes the drawer from the scrim and after choosing a link', async () => {
    const { fixture, el } = await render();
    const toggle = el.querySelector('button.menu-toggle') as HTMLButtonElement;
    toggle.click();
    fixture.detectChanges();
    (el.querySelector('.scrim') as HTMLElement).click();
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    toggle.click();
    fixture.detectChanges();
    (el.querySelector('nav a') as HTMLAnchorElement).click();
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
  });

  it('has no axe violations', async () => {
    const { el } = await render();
    await expectNoAxeViolations(el, { region: true });
  });
});
