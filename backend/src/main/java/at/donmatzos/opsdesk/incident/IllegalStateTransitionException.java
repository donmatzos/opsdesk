package at.donmatzos.opsdesk.incident;

public class IllegalStateTransitionException extends RuntimeException {

    public IllegalStateTransitionException(IncidentState from, IncidentState to) {
        super("Cannot change incident state from " + from + " to " + to);
    }
}
