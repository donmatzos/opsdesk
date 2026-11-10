package at.donmatzos.opsdesk.metrics;

import at.donmatzos.opsdesk.incident.IncidentOpened;
import at.donmatzos.opsdesk.incident.IncidentRepository;
import at.donmatzos.opsdesk.incident.IncidentState;
import at.donmatzos.opsdesk.incident.IncidentStateChanged;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Business metrics of the incident desk. Counters and timers react to committed domain events only; the number of
 * active incidents is read from the database on each scrape, so it is correct after restarts and across replicas.
 */
@Component
public class IncidentMetrics {

    private final MeterRegistry registry;

    public IncidentMetrics(MeterRegistry registry, IncidentRepository incidents) {
        this.registry = registry;
        for (IncidentState state : new IncidentState[] {IncidentState.OPEN, IncidentState.MITIGATED}) {
            Gauge.builder("opsdesk.incidents.active", incidents, repo -> repo.countByState(state))
                    .description("Incidents that are not resolved yet")
                    .tag("state", state.name())
                    .register(registry);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(IncidentOpened event) {
        Counter.builder("opsdesk.incidents.opened")
                .description("Opened incidents")
                .tag("severity", event.severity().name())
                .register(registry)
                .increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(IncidentStateChanged event) {
        if (event.to() != IncidentState.RESOLVED) {
            return;
        }
        Timer.builder("opsdesk.incidents.time.to.resolve")
                .description("Time from opening to resolving an incident (MTTR = sum / count)")
                .tag("severity", event.severity().name())
                .publishPercentileHistogram()
                .register(registry)
                .record(Duration.between(event.openedAt(), event.changedAt()));
    }
}
