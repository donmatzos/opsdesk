package at.donmatzos.opsdesk.incident;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.time.Instant;
import org.testng.annotations.Test;

public class IncidentTest {

    private static final Instant OPENED = Instant.parse("2026-11-10T08:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-10T09:30:00Z");

    private final Incident open = Incident.open(1L, Severity.SEV2, "Checkout returns 502", OPENED);

    @Test
    public void shouldOpenWithoutResolvedAt() {
        assertEquals(open.state(), IncidentState.OPEN);
        assertEquals(open.openedAt(), OPENED);
        assertNull(open.resolvedAt());
    }

    @Test
    public void shouldSetResolvedAtWhenResolved() {
        assertEquals(open.transitionTo(IncidentState.RESOLVED, LATER).resolvedAt(), LATER);
    }

    @Test
    public void shouldKeepResolvedAtNullWhenMitigated() {
        Incident mitigated = open.transitionTo(IncidentState.MITIGATED, LATER);
        assertEquals(mitigated.state(), IncidentState.MITIGATED);
        assertNull(mitigated.resolvedAt());
    }

    @Test(expectedExceptions = IllegalStateTransitionException.class,
            expectedExceptionsMessageRegExp = "Cannot change incident state from RESOLVED to OPEN")
    public void shouldRejectReopeningResolvedIncident() {
        open.transitionTo(IncidentState.RESOLVED, LATER).transitionTo(IncidentState.OPEN, LATER);
    }
}
