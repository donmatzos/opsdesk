package at.donmatzos.opsdesk.incident;

import static at.donmatzos.opsdesk.incident.IncidentState.MITIGATED;
import static at.donmatzos.opsdesk.incident.IncidentState.OPEN;
import static at.donmatzos.opsdesk.incident.IncidentState.RESOLVED;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class IncidentStateTest {

    @DataProvider
    public Object[][] transitions() {
        return new Object[][] {
                {OPEN, OPEN, false}, {OPEN, MITIGATED, true}, {OPEN, RESOLVED, true},
                {MITIGATED, OPEN, false}, {MITIGATED, MITIGATED, false}, {MITIGATED, RESOLVED, true},
                {RESOLVED, OPEN, false}, {RESOLVED, MITIGATED, false}, {RESOLVED, RESOLVED, false},
        };
    }

    @Test(dataProvider = "transitions")
    public void shouldAllowOnlyForwardTransitions(IncidentState from, IncidentState to, boolean expected) {
        assertEquals(from.canTransitionTo(to), expected, from + " -> " + to);
    }
}
