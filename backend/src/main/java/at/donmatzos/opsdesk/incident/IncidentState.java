package at.donmatzos.opsdesk.incident;

/** Lifecycle of an incident. It only moves forward; a resolved incident is never reopened (open a new one). */
public enum IncidentState {
    OPEN, MITIGATED, RESOLVED;

    public boolean canTransitionTo(IncidentState next) {
        return switch (this) {
            case OPEN -> next == MITIGATED || next == RESOLVED;
            case MITIGATED -> next == RESOLVED;
            case RESOLVED -> false;
        };
    }
}
