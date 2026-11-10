package at.donmatzos.opsdesk.metrics;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import at.donmatzos.opsdesk.IntegrationTest;
import at.donmatzos.opsdesk.catalog.MonitoredServiceRepository;
import at.donmatzos.opsdesk.incident.CreateIncidentRequest;
import at.donmatzos.opsdesk.incident.IncidentResponse;
import at.donmatzos.opsdesk.incident.IncidentService;
import at.donmatzos.opsdesk.incident.Severity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;
import org.testng.annotations.Test;

public class MetricsEndpointTest extends IntegrationTest {

    @Autowired
    private MonitoredServiceRepository services;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private MeterRegistry registry;

    private long serviceId() {
        return services.findAllByOrderByNameAsc().getFirst().id();
    }

    @Test
    public void shouldExposeIncidentMetricsForPrometheus() {
        IncidentResponse created = client().post().uri("/api/incidents").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("serviceId", serviceId(), "severity", "SEV2", "summary", "Metrics check"))
                .exchange().expectStatus().isCreated()
                .expectBody(IncidentResponse.class).returnResult().getResponseBody();
        client().patch().uri("/api/incidents/{id}", created.id()).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("state", "RESOLVED")).exchange().expectStatus().isOk();

        String body = client().get().uri("/actuator/prometheus").exchange()
                .expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();

        assertTrue(body.contains("opsdesk_incidents_opened_total{application=\"opsdesk\",severity=\"SEV2\"}"), body);
        assertTrue(body.contains("opsdesk_incidents_time_to_resolve_seconds_count{application=\"opsdesk\",severity=\"SEV2\"}"));
        assertTrue(body.contains("opsdesk_incidents_active{application=\"opsdesk\",state=\"OPEN\"}"));
    }

    @Test
    public void shouldExposeOnlyHealthInfoAndPrometheus() {
        client().get().uri("/actuator/env").exchange().expectStatus().isNotFound();
        client().get().uri("/actuator/beans").exchange().expectStatus().isNotFound();
    }

    // Review Focus 1: metrics count committed changes only.
    @Test
    public void shouldNotCountWhenTransactionRollsBack() {
        double before = openedSev3();

        transactions.executeWithoutResult(status -> {
            incidentService.open(new CreateIncidentRequest(serviceId(), Severity.SEV3, "Rolled back"));
            status.setRollbackOnly();
        });

        assertEquals(openedSev3(), before);
    }

    private double openedSev3() {
        Counter counter = registry.find("opsdesk.incidents.opened").tag("severity", "SEV3").counter();
        return counter == null ? 0.0 : counter.count();
    }
}
