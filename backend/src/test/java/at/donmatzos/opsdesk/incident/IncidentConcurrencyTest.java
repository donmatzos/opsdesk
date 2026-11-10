package at.donmatzos.opsdesk.incident;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import at.donmatzos.opsdesk.IntegrationTest;
import at.donmatzos.opsdesk.catalog.MonitoredServiceRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.testng.annotations.Test;

/** Two requests that read the same incident must not both win: the second write is rejected. */
public class IncidentConcurrencyTest extends IntegrationTest {

    @Autowired
    private IncidentRepository incidents;

    @Autowired
    private MonitoredServiceRepository services;

    @Test
    public void shouldRejectStaleUpdateAfterConcurrentTransition() {
        long serviceId = services.findAllByOrderByNameAsc().getFirst().id();
        Instant now = Instant.now();
        long id = incidents.save(Incident.open(serviceId, Severity.SEV2, "Race", now)).id();

        Incident readByA = incidents.findById(id).orElseThrow();
        Incident readByB = incidents.findById(id).orElseThrow();
        incidents.save(readByA.transitionTo(IncidentState.RESOLVED, now));

        assertThrows(OptimisticLockingFailureException.class,
                () -> incidents.save(readByB.transitionTo(IncidentState.MITIGATED, now)));
        assertEquals(incidents.findById(id).orElseThrow().state(), IncidentState.RESOLVED);
    }
}
