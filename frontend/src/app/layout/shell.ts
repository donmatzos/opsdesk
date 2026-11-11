import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { environment } from '../../environments/environment';
import { Icon } from '../shared/icon';
import { FocusOnNavigate } from './focus-on-navigate';
import { NAV_GROUPS } from './nav-items';

/** Landmarks, skip link, navigation (sidebar from 1024 px, disclosure menu below) and the focusable <main>. */
@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, Icon],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly focus = inject(FocusOnNavigate);

  protected readonly groups = NAV_GROUPS;
  protected readonly demo = environment.demo;
  protected readonly repoUrl = environment.repoUrl;
  protected readonly menuOpen = signal(false);

  protected skipToMain(event: Event): void {
    event.preventDefault(); // href="#main" would resolve against <base href> and leave the route
    this.focus.focusMain();
  }

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }
}
