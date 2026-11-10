package at.donmatzos.opsdesk.metrics;

import at.donmatzos.opsdesk.incident.IncidentOpened;
import at.donmatzos.opsdesk.incident.IncidentRepository;
import at.donmatzos.opsdesk.incident.IncidentState;
import at.donmatzos.opsdesk.incident.IncidentStateChanged;
import at.donmatzos.opsdesk.incident.Severity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Business metrics of the incident desk.
 * <ul>
 *   <li>Counters and timers react to committed domain events only, and exist for every severity from the start
 *       (so the first increment is visible to rate()/increase()).</li>
 *   <li>Active incidents are counted in the database on a schedule, not per scrape: a scrape never waits for the
 *       database, and an unreachable database shows up as NaN instead of a stale number. The value is global, so
 *       across replicas aggregate with max, not sum.</li>
 * </ul>
 */
@Component
public class IncidentMetrics {

    private static final Logger log = LoggerFactory.getLogger(IncidentMetrics.class);
    private static final IncidentState[] ACTIVE = {IncidentState.OPEN, IncidentState.MITIGATED};

    private final IncidentRepository incidents;
    private final Map<Severity, Counter> opened = new EnumMap<>(Severity.class);
    private final Map<Severity, Timer> timeToResolve = new EnumMap<>(Severity.class);
    private final Map<IncidentState, AtomicReference<Double>> active = new EnumMap<>(IncidentState.class);

    public IncidentMetrics(MeterRegistry registry, IncidentRepository incidents) {
        this.incidents = incidents;
        for (Severity severity : Severity.values()) {
            opened.put(severity, Counter.builder("opsdesk.incidents.opened")
                    .description("Opened incidents")
                    .tag("severity", severity.name())
                    .register(registry));
            // SLO buckets for resolve times of minutes to days (the default histogram stops at 30 s).
            timeToResolve.put(severity, Timer.builder("opsdesk.incidents.time.to.resolve")
                    .description("Time from opening to resolving an incident (MTTR = sum / count)")
                    .tag("severity", severity.name())
                    .serviceLevelObjectives(Duration.ofMinutes(15), Duration.ofHours(1), Duration.ofHours(4),
                            Duration.ofDays(1), Duration.ofDays(7))
                    .register(registry));
        }
        for (IncidentState state : ACTIVE) {
            AtomicReference<Double> value = new AtomicReference<>(Double.NaN);
            active.put(state, value);
            Gauge.builder("opsdesk.incidents.active", value, AtomicReference::get)
                    .description("Incidents that are not resolved yet (refreshed from the database)")
                    .tag("state", state.name())
                    .register(registry);
        }
    }

    @Scheduled(initialDelayString = "${opsdesk.metrics.active-refresh-initial-delay:PT5S}",
            fixedDelayString = "${opsdesk.metrics.active-refresh:PT15S}")
    public void refreshActiveIncidents() {
        for (IncidentState state : ACTIVE) {
            try {
                active.get(state).set((double) incidents.countByState(state));
            } catch (RuntimeException e) {
                active.get(state).set(Double.NaN);
                log.warn("Could not refresh the active incident count for {}: {}", state, e.getMessage());
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(IncidentOpened event) {
        opened.get(event.severity()).increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(IncidentStateChanged event) {
        if (event.to() == IncidentState.RESOLVED) {
            timeToResolve.get(event.severity()).record(Duration.between(event.openedAt(), event.changedAt()));
        }
    }
}
