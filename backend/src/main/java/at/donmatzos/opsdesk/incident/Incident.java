package at.donmatzos.opsdesk.incident;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Immutable incident; state changes return a new instance. {@code version} enables optimistic locking, so two
 * concurrent state changes cannot both win (the stale one fails instead of overwriting).
 */
@Table("incidents")
public record Incident(@Id Long id, Long serviceId, Severity severity, IncidentState state, String summary,
                       Instant openedAt, Instant resolvedAt, @Version @JsonIgnore Long version) {

    public static Incident open(long serviceId, Severity severity, String summary, Instant now) {
        return new Incident(null, serviceId, severity, IncidentState.OPEN, summary, now, null, null);
    }

    public Incident transitionTo(IncidentState next, Instant now) {
        if (!state.canTransitionTo(next)) {
            throw new IllegalStateTransitionException(state, next);
        }
        Instant resolved = next == IncidentState.RESOLVED ? now : resolvedAt;
        return new Incident(id, serviceId, severity, next, summary, openedAt, resolved, version);
    }
}
