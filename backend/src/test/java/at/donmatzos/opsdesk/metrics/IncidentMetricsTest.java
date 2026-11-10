package at.donmatzos.opsdesk.metrics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.testng.Assert.assertTrue;

import at.donmatzos.opsdesk.incident.IncidentOpened;
import at.donmatzos.opsdesk.incident.IncidentRepository;
import at.donmatzos.opsdesk.incident.IncidentState;
import at.donmatzos.opsdesk.incident.IncidentStateChanged;
import at.donmatzos.opsdesk.incident.Severity;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Plain unit test: in-memory meter registry, repository mocked. */
public class IncidentMetricsTest {

    private static final Instant OPENED = Instant.parse("2026-11-10T08:00:00Z");

    private SimpleMeterRegistry registry;
    private IncidentRepository incidents;
    private IncidentMetrics metrics;

    @BeforeMethod
    public void setUp() {
        registry = new SimpleMeterRegistry();
        incidents = mock(IncidentRepository.class);
        metrics = new IncidentMetrics(registry, incidents);
    }

    @Test
    public void shouldCountOpenedIncidentsPerSeverity() {
        metrics.on(new IncidentOpened(1L, Severity.SEV1));
        metrics.on(new IncidentOpened(2L, Severity.SEV1));
        metrics.on(new IncidentOpened(3L, Severity.SEV3));

        assertEquals(registry.get("opsdesk.incidents.opened").tag("severity", "SEV1").counter().count(), 2.0);
        assertEquals(registry.get("opsdesk.incidents.opened").tag("severity", "SEV3").counter().count(), 1.0);
    }

    @Test
    public void shouldRecordTimeToResolveOnlyWhenResolved() {
        metrics.on(change(IncidentState.OPEN, IncidentState.MITIGATED, 30));
        assertEquals(registry.get("opsdesk.incidents.time.to.resolve").tag("severity", "SEV2").timer().count(), 0L);

        metrics.on(change(IncidentState.MITIGATED, IncidentState.RESOLVED, 90));
        var timer = registry.get("opsdesk.incidents.time.to.resolve").tag("severity", "SEV2").timer();
        assertEquals(timer.count(), 1L);
        assertEquals(timer.totalTime(TimeUnit.SECONDS), 5400.0);
    }

    @Test
    public void shouldReadActiveIncidentsFromTheRepositoryOnRefresh() {
        when(incidents.countByState(IncidentState.OPEN)).thenReturn(3L);
        when(incidents.countByState(IncidentState.MITIGATED)).thenReturn(1L);

        metrics.refreshActiveIncidents();

        assertEquals(registry.get("opsdesk.incidents.active").tag("state", "OPEN").gauge().value(), 3.0);
        assertEquals(registry.get("opsdesk.incidents.active").tag("state", "MITIGATED").gauge().value(), 1.0);
    }

    // A scrape never queries the database; it reads the last refreshed value.
    @Test
    public void shouldNotQueryTheDatabaseOnScrape() {
        registry.get("opsdesk.incidents.active").tag("state", "OPEN").gauge().value();
        verifyNoInteractions(incidents);
    }

    @Test
    public void shouldReportNaNWhenTheDatabaseIsUnavailable() {
        when(incidents.countByState(IncidentState.OPEN)).thenReturn(3L);
        metrics.refreshActiveIncidents();
        when(incidents.countByState(IncidentState.OPEN)).thenThrow(new IllegalStateException("db down"));

        metrics.refreshActiveIncidents();

        assertTrue(Double.isNaN(registry.get("opsdesk.incidents.active").tag("state", "OPEN").gauge().value()));
    }

    // Pre-registered: the first increment is visible to rate()/increase(), Datadog sees 0 instead of no data.
    @Test
    public void shouldRegisterCountersAndTimersForEverySeverityUpFront() {
        for (Severity severity : Severity.values()) {
            assertEquals(registry.get("opsdesk.incidents.opened").tag("severity", severity.name()).counter().count(), 0.0);
            assertEquals(registry.get("opsdesk.incidents.time.to.resolve").tag("severity", severity.name()).timer().count(), 0L);
        }
    }

    // Resolve times are minutes to days: SLO buckets instead of the default 1 ms - 30 s histogram.
    @Test
    public void shouldBucketTimeToResolveBySlo() {
        var buckets = registry.get("opsdesk.incidents.time.to.resolve").tag("severity", "SEV1").timer()
                .takeSnapshot().histogramCounts();
        assertEquals(buckets.length, 5);
        assertEquals(buckets[0].bucket(TimeUnit.MINUTES), 15.0);
        assertEquals(buckets[4].bucket(TimeUnit.DAYS), 7.0);
    }

    private static IncidentStateChanged change(IncidentState from, IncidentState to, long minutes) {
        return new IncidentStateChanged(7L, Severity.SEV2, from, to, OPENED, OPENED.plusSeconds(minutes * 60));
    }
}
