import { Component, ElementRef, HostListener, Injector, afterNextRender, inject, signal, viewChild } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { environment } from '../../environments/environment';
import { Icon } from '../shared/icon';
import { FocusOnNavigate } from './focus-on-navigate';
import { NAV_GROUPS } from './nav-items';
import { ThemeToggle } from './theme-toggle';

/**
 * Landmarks, skip link, sticky header with the theme toggle, a sticky full-height sidebar from 1024 px, and an
 * off-canvas drawer below (scrim, Escape, closes on navigation, <main> inert while open).
 */
@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, Icon, ThemeToggle],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly focus = inject(FocusOnNavigate);
  private readonly injector = inject(Injector);
  private readonly toggleButton = viewChild.required<ElementRef<HTMLButtonElement>>('toggleButton');
  private readonly nav = viewChild.required<ElementRef<HTMLElement>>('siteNav');

  protected readonly groups = NAV_GROUPS;
  protected readonly demo = environment.demo;
  protected readonly repoUrl = environment.repoUrl;
  protected readonly menuOpen = signal(false);

  protected skipToMain(event: Event): void {
    event.preventDefault(); // href="#main" would resolve against <base href> and leave the route
    this.focus.focusMain();
  }

  protected toggleMenu(): void {
    const open = !this.menuOpen();
    this.menuOpen.set(open);
    if (open) {
      afterNextRender(() => this.nav().nativeElement.querySelector<HTMLElement>('a')?.focus(), { injector: this.injector });
    }
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    if (this.menuOpen()) {
      this.closeMenu();
      this.toggleButton().nativeElement.focus();
    }
  }

  /** The drawer only exists below 1024 px; growing the window closes it. */
  @HostListener('window:resize')
  protected onResize(): void {
    if (window.innerWidth >= 1024) {
      this.closeMenu();
    }
  }
}
