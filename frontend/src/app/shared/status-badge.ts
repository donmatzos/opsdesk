import { Component, computed, input } from '@angular/core';
import { Icon } from './icon';

type Tone = 'ok' | 'warn' | 'bad' | 'info' | 'muted';

const BADGES: Record<string, { icon: string; tone: Tone }> = {
  UP: { icon: 'circle-check', tone: 'ok' },
  DEGRADED: { icon: 'triangle-alert', tone: 'warn' },
  DOWN: { icon: 'circle-x', tone: 'bad' },
  OPEN: { icon: 'circle-dot', tone: 'bad' },
  MITIGATED: { icon: 'triangle-alert', tone: 'info' },
  RESOLVED: { icon: 'circle-check', tone: 'ok' },
};
const SEVERITIES: Record<string, Tone> = { SEV1: 'bad', SEV2: 'warn', SEV3: 'muted' };

/**
 * Status (service status, incident state) is a pill with icon + text; severity is a square tag with text only, so the
 * two never look alike. Status is never conveyed by colour alone.
 */
@Component({
  selector: 'app-status-badge',
  imports: [Icon],
  template: `@if (severity()) {{{ value() }}} @else {<app-icon [name]="icon()" />{{ value() }}}`,
  host: { '[class]': 'classes()' },
  styles: `
    :host { display: inline-flex; align-items: center; gap: 0.375rem; font-weight: 600; font-size: 0.8125rem; white-space: nowrap; line-height: 1; }
    :host(.pill) { padding: 0.3125rem 0.625rem 0.3125rem 0.5rem; border-radius: 999px; }
    :host(.tag) { padding: 0.3125rem 0.5rem; border-radius: 0.375rem; font-family: var(--font-mono); letter-spacing: 0.02em; }
    :host(.ok) { background: var(--ok-soft); color: var(--ok); }
    :host(.warn) { background: var(--warning-soft); color: var(--warning); }
    :host(.bad) { background: var(--danger-soft); color: var(--danger); }
    :host(.info) { background: var(--info-soft); color: var(--info); }
    :host(.muted) { background: var(--surface-2); color: var(--muted); }
  `,
})
export class StatusBadge {
  readonly value = input.required<string>();
  protected readonly severity = computed(() => this.value() in SEVERITIES);
  protected readonly icon = computed(() => BADGES[this.value()]?.icon ?? 'circle-dot');
  protected readonly tone = computed(() => SEVERITIES[this.value()] ?? BADGES[this.value()]?.tone ?? 'info');
  protected readonly classes = computed(() => `${this.severity() ? 'tag' : 'pill'} ${this.tone()}`);
}
