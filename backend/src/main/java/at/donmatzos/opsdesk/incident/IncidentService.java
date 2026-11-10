package at.donmatzos.opsdesk.incident;

import at.donmatzos.opsdesk.catalog.MonitoredServiceRepository;
import at.donmatzos.opsdesk.common.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidents;
    private final MonitoredServiceRepository services;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    IncidentService(IncidentRepository incidents, MonitoredServiceRepository services, Clock clock,
                    ApplicationEventPublisher events) {
        this.incidents = incidents;
        this.services = services;
        this.clock = clock;
        this.events = events;
    }

    @Transactional
    public Incident open(CreateIncidentRequest request) {
        // Checked up front so the client gets a clear 422 instead of a foreign-key violation.
        if (!services.existsById(request.serviceId())) {
            throw new UnknownServiceException(request.serviceId());
        }
        Incident opened = incidents.save(
                Incident.open(request.serviceId(), request.severity(), request.summary(), clock.instant()));
        events.publishEvent(new IncidentOpened(opened.id(), opened.severity()));
        // Key-value pairs become ECS fields (incident.id, ...), so Kibana can filter on them.
        log.atInfo().setMessage("Incident opened")
                .addKeyValue("incident.id", opened.id())
                .addKeyValue("incident.severity", opened.severity())
                .addKeyValue("incident.service_id", opened.serviceId())
                .log();
        return opened;
    }

    @Transactional(readOnly = true)
    public List<Incident> list(@Nullable IncidentState state) {
        return state == null ? incidents.findAllByOrderByOpenedAtDesc() : incidents.findByStateOrderByOpenedAtDesc(state);
    }

    @Transactional(readOnly = true)
    public Incident get(long id) {
        return incidents.findById(id).orElseThrow(() -> new NotFoundException("Incident", id));
    }

    @Transactional
    public Incident changeState(long id, IncidentState next) {
        Incident current = get(id);
        Instant now = clock.instant();
        Incident changed = incidents.save(current.transitionTo(next, now));
        events.publishEvent(new IncidentStateChanged(changed.id(), changed.severity(), current.state(), next,
                current.openedAt(), now));
        log.atInfo().setMessage("Incident state changed")
                .addKeyValue("incident.id", changed.id())
                .addKeyValue("incident.severity", changed.severity())
                .addKeyValue("incident.from", current.state())
                .addKeyValue("incident.to", next)
                .log();
        return changed;
    }
}
