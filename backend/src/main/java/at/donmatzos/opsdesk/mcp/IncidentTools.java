package at.donmatzos.opsdesk.mcp;

import at.donmatzos.opsdesk.catalog.MonitoredService;
import at.donmatzos.opsdesk.catalog.MonitoredServiceRepository;
import at.donmatzos.opsdesk.incident.CreateIncidentRequest;
import at.donmatzos.opsdesk.incident.Incident;
import at.donmatzos.opsdesk.incident.IncidentService;
import at.donmatzos.opsdesk.incident.IncidentState;
import at.donmatzos.opsdesk.incident.Severity;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP tools over service status and incidents. They call {@link IncidentService}, so validation, state
 * transitions, after-commit metrics and ECS logs behave exactly as for the REST API. Parameter identifiers are
 * snake_case because the MCP argument name is taken from the Java parameter name.
 */
@Component
class IncidentTools {

    /** Compact view of an incident; the service is given by name (the model never sees numeric service ids). */
    record IncidentSummary(long id, String service, Severity severity, IncidentState state, String summary,
                           Instant openedAt, Instant resolvedAt) {
    }

    record IncidentList(List<IncidentSummary> incidents, boolean truncated) {
    }

    private static final int MAX_SUMMARY_LENGTH = 200;
    private static final String SEVERITIES = names(Severity.values());
    private static final String STATES = names(IncidentState.values());

    private final IncidentService incidents;
    private final MonitoredServiceRepository services;

    IncidentTools(IncidentService incidents, MonitoredServiceRepository services) {
        this.incidents = incidents;
        this.services = services;
    }

    @McpTool(name = "get_service_status",
            description = """
                    Returns the current status of one monitored service: id, name, ownerTeam, stage (DEV or PROD) \
                    and status (UP, DEGRADED or DOWN). Call this to check whether a specific service is healthy. \
                    If the name is unknown, the result is an error; call list_services for valid names. Read-only.""",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    CallToolResult getServiceStatus(
            @McpToolParam(description = "Exact service name, for example billing-api. Case-sensitive.")
            String service_name) {
        return McpResults.run(() -> services.findByName(service_name)
                .map(s -> McpResults.ok(new CatalogTools.ServiceSummary(s.id(), s.name(), s.ownerTeam(), s.stage(),
                        s.status())))
                .orElseGet(() -> McpResults.error(
                        "Unknown service '" + service_name + "'. Call list_services to see valid names.")));
    }

    @McpTool(name = "list_active_incidents",
            description = """
                    Lists incidents that are not resolved yet (state OPEN or MITIGATED), newest first. Result: \
                    {incidents: [{id, service, severity, state, summary, openedAt, resolvedAt}], truncated}. At \
                    most 50 entries are returned; truncated is true if there are more (narrow down with severity). \
                    Call this to see what is currently going wrong. Read-only.""",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    CallToolResult listActiveIncidents(
            @McpToolParam(required = false, description = """
                    Only return incidents of this severity. Valid values: SEV1 (most severe), SEV2, SEV3. Omit to \
                    return all severities.""")
            String severity) {
        return McpResults.run(() -> {
            boolean filtered = severity != null && !severity.isBlank();
            Severity wanted = filtered ? parse(Severity.class, severity) : null;
            if (filtered && wanted == null) {
                return invalid("severity", severity, SEVERITIES);
            }
            List<Incident> active = Stream.of(IncidentState.OPEN, IncidentState.MITIGATED)
                    .flatMap(state -> incidents.list(state).stream())
                    .filter(i -> wanted == null || i.severity() == wanted)
                    .sorted(Comparator.comparing(Incident::openedAt).reversed())
                    .toList();
            var capped = McpResults.cap(active);
            Map<Long, String> names = serviceNames();
            return McpResults.ok(new IncidentList(
                    capped.items().stream().map(i -> summary(i, names)).toList(), capped.truncated()));
        });
    }

    @McpTool(name = "get_incident",
            description = """
                    Returns one incident by id: {id, service, severity, state, summary, openedAt, resolvedAt}. \
                    resolvedAt is null until the incident is RESOLVED. Call this to inspect an incident you found \
                    with list_active_incidents or just opened. Unknown ids return an error. Read-only.""",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    CallToolResult getIncident(
            @McpToolParam(description = "Numeric incident id, as returned by list_active_incidents or open_incident.")
            long incident_id) {
        return McpResults.run(() -> McpResults.ok(summary(incidents.get(incident_id), serviceNames())));
    }

    @McpTool(name = "open_incident",
            description = """
                    Opens a new incident (state OPEN) for a service and returns it as {id, service, severity, \
                    state, summary, openedAt, resolvedAt}. This writes data and counts in the incident metrics: \
                    call it only when the user asks to report a problem, and not twice for the same problem. \
                    Unknown service names, invalid severities and invalid summaries return an error.""",
            annotations = @McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false,
                    openWorldHint = false))
    CallToolResult openIncident(
            @McpToolParam(description = "Exact service name from list_services, for example billing-api.")
            String service_name,
            @McpToolParam(description = "Severity. Valid values: SEV1 (most severe), SEV2, SEV3.")
            String severity,
            @McpToolParam(description = "Short description of the problem, 1 to 200 characters.")
            String summary) {
        return McpResults.run(() -> {
            Severity parsed = severity == null ? null : parse(Severity.class, severity);
            if (parsed == null) {
                return invalid("severity", severity, SEVERITIES);
            }
            if (summary == null || summary.isBlank() || summary.length() > MAX_SUMMARY_LENGTH) {
                return McpResults.error("Invalid summary: it must be 1 to " + MAX_SUMMARY_LENGTH + " characters.");
            }
            MonitoredService service = services.findByName(service_name).orElse(null);
            if (service == null) {
                return McpResults.error(
                        "Unknown service '" + service_name + "'. Call list_services to see valid names.");
            }
            Incident opened = incidents.open(new CreateIncidentRequest(service.id(), parsed, summary));
            return McpResults.ok(summary(opened, Map.of(service.id(), service.name())));
        });
    }

    @McpTool(name = "change_incident_state",
            description = """
                    Moves an incident to a new state and returns it as {id, service, severity, state, summary, \
                    openedAt, resolvedAt}. Allowed transitions: OPEN -> MITIGATED, MITIGATED -> RESOLVED, OPEN -> \
                    RESOLVED. A RESOLVED incident cannot be reopened (open a new incident instead); illegal \
                    transitions and unknown ids return an error. Writes data; it is not safe to repeat blindly.""",
            annotations = @McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false,
                    openWorldHint = false))
    CallToolResult changeIncidentState(
            @McpToolParam(description = "Numeric incident id, as returned by list_active_incidents or open_incident.")
            long incident_id,
            @McpToolParam(description = "Target state. Valid values: OPEN, MITIGATED, RESOLVED (see the allowed "
                    + "transitions in the tool description).")
            String state) {
        return McpResults.run(() -> {
            IncidentState parsed = state == null ? null : parse(IncidentState.class, state);
            if (parsed == null) {
                return invalid("state", state, STATES);
            }
            return McpResults.ok(summary(incidents.changeState(incident_id, parsed), serviceNames()));
        });
    }

    private static CallToolResult invalid(String field, String value, String valid) {
        return McpResults.error("Invalid " + field + " '" + value + "'. Valid values: " + valid + ".");
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        return Arrays.stream(type.getEnumConstants())
                .filter(e -> e.name().equalsIgnoreCase(value.trim())).findFirst().orElse(null);
    }

    private static String names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.joining(", "));
    }

    private Map<Long, String> serviceNames() {
        Map<Long, String> names = new HashMap<>();
        services.findAll().forEach(s -> names.put(s.id(), s.name()));
        return names;
    }

    private static IncidentSummary summary(Incident i, Map<Long, String> names) {
        return new IncidentSummary(i.id(), names.getOrDefault(i.serviceId(), "service-" + i.serviceId()),
                i.severity(), i.state(), i.summary(), i.openedAt(), i.resolvedAt());
    }
}
