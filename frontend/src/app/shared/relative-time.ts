const MINUTE = 60_000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

/** "just now", "5 min ago", "3 h ago", "2 d ago"; older than 30 days falls back to the date. */
export function relativeTime(iso: string, now: number = Date.now()): string {
  const diff = now - new Date(iso).getTime();
  if (Number.isNaN(diff)) {
    return '';
  }
  if (diff < MINUTE) {
    return 'just now';
  }
  if (diff < HOUR) {
    return `${Math.floor(diff / MINUTE)} min ago`;
  }
  if (diff < DAY) {
    return `${Math.floor(diff / HOUR)} h ago`;
  }
  if (diff < 30 * DAY) {
    return `${Math.floor(diff / DAY)} d ago`;
  }
  return iso.slice(0, 10);
}
