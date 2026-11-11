import { relativeTime } from './relative-time';

const NOW = Date.parse('2026-11-11T12:00:00Z');
const ago = (ms: number) => new Date(NOW - ms).toISOString();

describe('relativeTime', () => {
  it('reads naturally across the ranges', () => {
    expect(relativeTime(ago(10_000), NOW)).toBe('just now');
    expect(relativeTime(ago(5 * 60_000), NOW)).toBe('5 min ago');
    expect(relativeTime(ago(3 * 3_600_000), NOW)).toBe('3 h ago');
    expect(relativeTime(ago(2 * 86_400_000), NOW)).toBe('2 d ago');
    expect(relativeTime(ago(40 * 86_400_000), NOW)).toBe('2026-10-02');
  });

  it('returns an empty string for an invalid date', () => {
    expect(relativeTime('nope', NOW)).toBe('');
  });
});
