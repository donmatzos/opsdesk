import { MonitoredService } from '../catalog/catalog-api';
import { Incident } from '../incidents/incident-api';

// All data in the demo is fictional. Services mirror the demo rows of the real backend (db/demo/R__demo_data.sql).
export const DEMO_SERVICES: MonitoredService[] = [
  { id: 1, name: 'billing-api', ownerTeam: 'team-payments', stage: 'PROD', status: 'UP' },
  { id: 2, name: 'customer-portal', ownerTeam: 'team-web', stage: 'PROD', status: 'DEGRADED' },
  { id: 3, name: 'order-service', ownerTeam: 'team-orders', stage: 'PROD', status: 'UP' },
  { id: 4, name: 'report-batch', ownerTeam: 'team-data', stage: 'DEV', status: 'DOWN' },
];

const HOUR = 3_600_000;

/** Five incidents across all states and severities, with times relative to "now" so the demo never looks stale. */
export function demoIncidents(now: Date): Incident[] {
  const ago = (ms: number) => new Date(now.getTime() - ms).toISOString();
  return [
    { id: 1, serviceId: 4, severity: 'SEV1', state: 'OPEN', summary: 'Nightly batch has not finished since 02:00', openedAt: ago(3 * HOUR), resolvedAt: null },
    { id: 2, serviceId: 2, severity: 'SEV2', state: 'MITIGATED', summary: 'Checkout page loads slowly, cache warmed as a workaround', openedAt: ago(26 * HOUR), resolvedAt: null },
    { id: 3, serviceId: 3, severity: 'SEV3', state: 'OPEN', summary: 'Retry rate for payment callbacks above normal', openedAt: ago(5 * HOUR), resolvedAt: null },
    { id: 4, serviceId: 1, severity: 'SEV2', state: 'RESOLVED', summary: 'Invoice PDF generation failed for large accounts', openedAt: ago(50 * HOUR), resolvedAt: ago(49 * HOUR) },
    { id: 5, serviceId: 3, severity: 'SEV1', state: 'RESOLVED', summary: 'Database connection refused after maintenance', openedAt: ago(100 * HOUR), resolvedAt: ago(98.5 * HOUR) },
  ];
}

export interface DemoRunbook {
  runbook: string;
  title: string;
  section: string;
  keywords: string[];
  excerpt: string;
}

/** Canned runbook sections (titles and sections as in docs/runbooks) for the mock assistant. */
export const DEMO_RUNBOOKS: DemoRunbook[] = [
  { runbook: 'database-unavailable', title: 'Database unavailable', section: 'First checks', keywords: ['database', 'postgres', 'connection', 'jdbc', 'sql'], excerpt: 'Is the database pod running? Check SPRING_DATASOURCE_URL and that the password Secret matches the database.' },
  { runbook: 'pod-not-ready', title: 'Pod not ready', section: 'First checks', keywords: ['pod', 'ready', 'readiness', 'probe', 'crashloop', 'deploy', 'rollout'], excerpt: 'Run oc describe pod and read the Events; a missing Secret shows as CreateContainerConfigError.' },
  { runbook: 'http-errors', title: 'HTTP errors (5xx)', section: 'First checks', keywords: ['5xx', 'http', 'error', 'errors', 'status', 'requests'], excerpt: 'Group http.server.requests by uri and status, then check whether a deploy came right before the spike.' },
  { runbook: 'sev1-incident', title: 'SEV1 incident', section: 'Mitigation', keywords: ['sev1', 'outage', 'down', 'incident', 'rollback'], excerpt: 'Roll back first, investigate second: helm rollback opsdesk and wait for the rollout to finish.' },
  { runbook: 'mcp-token-rotation', title: 'MCP token rotation', section: 'Mitigation', keywords: ['token', 'mcp', 'rotate', 'rotation', '401', 'secret'], excerpt: 'Generate a new token, update the Secret, restart the deployment and verify with tools/mcp-verify.sh.' },
];
