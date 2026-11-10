package at.donmatzos.opsdesk.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.donmatzos.opsdesk.catalog.MonitoredService;
import at.donmatzos.opsdesk.catalog.MonitoredServiceRepository;
import at.donmatzos.opsdesk.catalog.ServiceStatus;
import at.donmatzos.opsdesk.catalog.Stage;
import at.donmatzos.opsdesk.incident.IllegalStateTransitionException;
import at.donmatzos.opsdesk.incident.Incident;
import at.donmatzos.opsdesk.incident.IncidentService;
import at.donmatzos.opsdesk.incident.IncidentState;
import at.donmatzos.opsdesk.incident.Severity;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Plain unit test: service and repository mocked, no Spring context. */
public class IncidentToolsTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Instant T0 = Instant.parse("2026-11-10T12:00:00Z");

    private IncidentService incidents;
    private MonitoredServiceRepository services;
    private IncidentTools tools;

    @BeforeMethod
    public void setUp() {
        incidents = mock(IncidentService.class);
        services = mock(MonitoredServiceRepository.class);
        tools = new IncidentTools(incidents, services);
        when(services.findAll()).thenReturn(List.of(new MonitoredService(1L, "billing-api", "payments", Stage.PROD,
                ServiceStatus.UP)));
    }

    private static JsonNode body(CallToolResult result) {
        return JSON.readTree(((TextContent) result.content().getFirst()).text());
    }

    private static String text(CallToolResult result) {
        return ((TextContent) result.content().getFirst()).text();
    }

    private static Incident incident(long id, Instant openedAt) {
        return new Incident(id, 1L, Severity.SEV2, IncidentState.OPEN, "slow", openedAt, null, 0L);
    }

    @Test
    public void shouldCapIncidentListsAt50() {
        List<Incident> sixty = IntStream.range(0, 60).mapToObj(i -> incident(i, T0.plusSeconds(i))).toList();
        when(incidents.list(IncidentState.OPEN)).thenReturn(sixty);
        when(incidents.list(IncidentState.MITIGATED)).thenReturn(List.of());

        CallToolResult result = tools.listActiveIncidents(null);

        assertThat(result.isError()).isFalse();
        assertThat(body(result).get("incidents")).hasSize(50);
        assertThat(body(result).get("truncated").asBoolean()).isTrue();
        // newest first
        assertThat(body(result).get("incidents").get(0).get("id").asLong()).isEqualTo(59);
    }

    @Test
    public void shouldNotFlagTruncationWhenUnderCap() {
        when(incidents.list(IncidentState.OPEN)).thenReturn(List.of(incident(1, T0)));
        when(incidents.list(IncidentState.MITIGATED)).thenReturn(List.of());

        CallToolResult result = tools.listActiveIncidents("SEV2");

        assertThat(body(result).get("incidents")).hasSize(1);
        assertThat(body(result).get("truncated").asBoolean()).isFalse();
        assertThat(body(result).get("incidents").get(0).get("service").asString()).isEqualTo("billing-api");
    }

    @Test
    public void shouldExplainUnknownService() {
        when(services.findByName("nope")).thenReturn(Optional.empty());

        CallToolResult result = tools.getServiceStatus("nope");

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).isEqualTo("Unknown service 'nope'. Call list_services to see valid names.");
    }

    @Test
    public void shouldExplainIllegalTransition() {
        when(incidents.changeState(7L, IncidentState.OPEN))
                .thenThrow(new IllegalStateTransitionException(IncidentState.RESOLVED, IncidentState.OPEN));

        CallToolResult result = tools.changeIncidentState(7L, "OPEN");

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).isEqualTo("Cannot change incident state from RESOLVED to OPEN");
    }

    @Test
    public void shouldRejectUnknownSeverity() {
        CallToolResult result = tools.openIncident("billing-api", "SEV9", "boom");

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("SEV9").contains("SEV1, SEV2, SEV3");
        verify(incidents, never()).open(any());
    }

    @Test
    public void shouldRejectUnknownState() {
        CallToolResult result = tools.changeIncidentState(7L, "DONE");

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("DONE").contains("OPEN, MITIGATED, RESOLVED");
    }

    @Test
    public void shouldRejectBlankSummary() {
        CallToolResult result = tools.openIncident("billing-api", "SEV1", "  ");

        assertThat(result.isError()).isTrue();
        verify(incidents, never()).open(any());
    }

    @Test
    public void shouldNotLeakUnexpectedErrors() {
        when(incidents.get(1L)).thenThrow(new IllegalStateException("select * from incidents -- secret"));

        CallToolResult result = tools.getIncident(1L);

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).doesNotContain("select");
    }
}
