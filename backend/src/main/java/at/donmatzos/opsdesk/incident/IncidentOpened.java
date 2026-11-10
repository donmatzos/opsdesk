package at.donmatzos.opsdesk.incident;

/** Published after an incident was opened (consumers react after the transaction commits). */
public record IncidentOpened(long incidentId, Severity severity) {
}
