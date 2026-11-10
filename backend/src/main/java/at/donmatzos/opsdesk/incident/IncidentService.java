package at.donmatzos.opsdesk.incident;

import at.donmatzos.opsdesk.catalog.MonitoredServiceRepository;
import at.donmatzos.opsdesk.common.NotFoundException;
import java.time.Clock;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {

    private final IncidentRepository incidents;
    private final MonitoredServiceRepository services;
    private final Clock clock;

    IncidentService(IncidentRepository incidents, MonitoredServiceRepository services, Clock clock) {
        this.incidents = incidents;
        this.services = services;
        this.clock = clock;
    }

    @Transactional
    public Incident open(CreateIncidentRequest request) {
        // Checked up front so the client gets a clear 422 instead of a foreign-key violation.
        if (!services.existsById(request.serviceId())) {
            throw new UnknownServiceException(request.serviceId());
        }
        return incidents.save(Incident.open(request.serviceId(), request.severity(), request.summary(), clock.instant()));
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
        return incidents.save(get(id).transitionTo(next, clock.instant()));
    }
}
