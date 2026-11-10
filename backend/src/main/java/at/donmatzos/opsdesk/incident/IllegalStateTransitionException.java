package at.donmatzos.opsdesk.incident;

import at.donmatzos.opsdesk.common.ConflictException;

public class IllegalStateTransitionException extends ConflictException {

    public IllegalStateTransitionException(IncidentState from, IncidentState to) {
        super("Cannot change incident state from " + from + " to " + to);
    }
}
