package at.donmatzos.opsdesk.incident;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Immutable incident; state changes return a new instance. */
@Table("incidents")
public record Incident(@Id Long id, Long serviceId, Severity severity, IncidentState state, String summary,
                       Instant openedAt, Instant resolvedAt) {

    public static Incident open(long serviceId, Severity severity, String summary, Instant now) {
        return new Incident(null, serviceId, severity, IncidentState.OPEN, summary, now, null);
    }

    public Incident transitionTo(IncidentState next, Instant now) {
        if (!state.canTransitionTo(next)) {
            throw new IllegalStateTransitionException(state, next);
        }
        Instant resolved = next == IncidentState.RESOLVED ? now : resolvedAt;
        return new Incident(id, serviceId, severity, next, summary, openedAt, resolved);
    }
}
