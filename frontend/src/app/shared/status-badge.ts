import { Component, computed, input } from '@angular/core';
import { Icon } from './icon';

type Tone = 'ok' | 'warn' | 'bad' | 'info';

const BADGES: Record<string, { icon: string; tone: Tone }> = {
  UP: { icon: 'circle-check', tone: 'ok' },
  DEGRADED: { icon: 'triangle-alert', tone: 'warn' },
  DOWN: { icon: 'circle-x', tone: 'bad' },
  OPEN: { icon: 'circle-dot', tone: 'bad' },
  MITIGATED: { icon: 'triangle-alert', tone: 'warn' },
  RESOLVED: { icon: 'circle-check', tone: 'ok' },
  SEV1: { icon: 'octagon-alert', tone: 'bad' },
  SEV2: { icon: 'triangle-alert', tone: 'warn' },
  SEV3: { icon: 'circle-dot', tone: 'info' },
};

/** Status is never conveyed by colour alone: icon + text + colour. */
@Component({
  selector: 'app-status-badge',
  imports: [Icon],
  template: `<app-icon [name]="icon()" />{{ value() }}`,
  host: { '[class]': 'tone()' },
  styles: `
    :host { display: inline-flex; align-items: center; gap: 0.375rem; font-weight: 600; font-size: 0.875rem; }
    :host(.ok) { color: var(--accent); }
    :host(.warn) { color: var(--warning); }
    :host(.bad) { color: var(--danger); }
    :host(.info) { color: var(--info); }
  `,
})
export class StatusBadge {
  readonly value = input.required<string>();
  protected readonly icon = computed(() => BADGES[this.value()]?.icon ?? 'circle-dot');
  protected readonly tone = computed(() => BADGES[this.value()]?.tone ?? 'info');
}
