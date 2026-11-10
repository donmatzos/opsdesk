package at.donmatzos.opsdesk.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import at.donmatzos.opsdesk.catalog.MonitoredServiceRepository;
import at.donmatzos.opsdesk.common.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Plain unit test: repositories mocked with Mockito, time fixed, no Spring context. */
public class IncidentServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

    private IncidentRepository incidents;
    private MonitoredServiceRepository services;
    private ApplicationEventPublisher events;
    private IncidentService service;

    @BeforeMethod
    public void setUp() {
        incidents = mock(IncidentRepository.class);
        services = mock(MonitoredServiceRepository.class);
        events = mock(ApplicationEventPublisher.class);
        service = new IncidentService(incidents, services, Clock.fixed(NOW, ZoneOffset.UTC), events);
        // Like the database: a new incident gets an id on save.
        when(incidents.save(any())).thenAnswer(invocation -> {
            Incident i = invocation.getArgument(0);
            return i.id() != null ? i
                    : new Incident(99L, i.serviceId(), i.severity(), i.state(), i.summary(), i.openedAt(), i.resolvedAt(), 0L);
        });
    }

    @Test
    public void shouldOpenIncidentAtCurrentTime() {
        when(services.existsById(1L)).thenReturn(true);

        service.open(new CreateIncidentRequest(1L, Severity.SEV1, "Database unreachable"));

        ArgumentCaptor<Incident> saved = ArgumentCaptor.forClass(Incident.class);
        verify(incidents).save(saved.capture());
        assertEquals(saved.getValue().state(), IncidentState.OPEN);
        assertEquals(saved.getValue().openedAt(), NOW);
    }

    @Test(expectedExceptions = UnknownServiceException.class)
    public void shouldNotSaveWhenServiceIsUnknown() {
        when(services.existsById(9L)).thenReturn(false);
        try {
            service.open(new CreateIncidentRequest(9L, Severity.SEV3, "x"));
        } finally {
            verify(incidents, never()).save(any());
        }
    }

    @Test
    public void shouldResolveWithCurrentTime() {
        Incident open = new Incident(5L, 1L, Severity.SEV2, IncidentState.OPEN, "x", NOW.minusSeconds(60), null, 0L);
        when(incidents.findById(5L)).thenReturn(Optional.of(open));

        assertEquals(service.changeState(5L, IncidentState.RESOLVED).resolvedAt(), NOW);
    }

    @Test(expectedExceptions = NotFoundException.class, expectedExceptionsMessageRegExp = "Incident 5 not found")
    public void shouldFailWhenIncidentIsUnknown() {
        when(incidents.findById(5L)).thenReturn(Optional.empty());
        service.changeState(5L, IncidentState.RESOLVED);
    }

    @Test
    public void shouldPublishIncidentOpened() {
        when(services.existsById(1L)).thenReturn(true);

        service.open(new CreateIncidentRequest(1L, Severity.SEV1, "Database unreachable"));

        verify(events).publishEvent(new IncidentOpened(99L, Severity.SEV1));
    }

    @Test
    public void shouldPublishStateChangeWithTimes() {
        Instant opened = NOW.minusSeconds(600);
        when(incidents.findById(5L)).thenReturn(Optional.of(
                new Incident(5L, 1L, Severity.SEV2, IncidentState.OPEN, "x", opened, null, 0L)));

        service.changeState(5L, IncidentState.RESOLVED);

        verify(events).publishEvent(new IncidentStateChanged(5L, Severity.SEV2, IncidentState.OPEN,
                IncidentState.RESOLVED, opened, NOW));
    }

    @Test
    public void shouldListAllWhenNoStateGiven() {
        service.list(null);
        verify(incidents).findAllByOrderByOpenedAtDesc();
        verify(incidents, never()).findByStateOrderByOpenedAtDesc(any());
    }

    @Test
    public void shouldFilterWhenStateGiven() {
        when(incidents.findByStateOrderByOpenedAtDesc(IncidentState.OPEN)).thenReturn(List.of());
        service.list(IncidentState.OPEN);
        verify(incidents).findByStateOrderByOpenedAtDesc(IncidentState.OPEN);
    }
}
