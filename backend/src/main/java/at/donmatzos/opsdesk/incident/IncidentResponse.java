package at.donmatzos.opsdesk.incident;

import java.time.Instant;

/** API view of an incident; keeps persistence details such as the lock version out of the JSON. */
public record IncidentResponse(long id, long serviceId, Severity severity, IncidentState state, String summary,
                               Instant openedAt, Instant resolvedAt) {

    static IncidentResponse from(Incident incident) {
        return new IncidentResponse(incident.id(), incident.serviceId(), incident.severity(), incident.state(),
                incident.summary(), incident.openedAt(), incident.resolvedAt());
    }
}
