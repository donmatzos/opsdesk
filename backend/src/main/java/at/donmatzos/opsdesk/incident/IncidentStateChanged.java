package at.donmatzos.opsdesk.incident;

import java.time.Instant;

/** Published after an incident moved to another state (consumers react after the transaction commits). */
public record IncidentStateChanged(long incidentId, Severity severity, IncidentState from, IncidentState to,
                                   Instant openedAt, Instant changedAt) {
}
