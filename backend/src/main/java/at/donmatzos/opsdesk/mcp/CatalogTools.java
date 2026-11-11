package at.donmatzos.opsdesk.mcp;

import at.donmatzos.opsdesk.catalog.MonitoredServiceRepository;
import at.donmatzos.opsdesk.catalog.ServiceStatus;
import at.donmatzos.opsdesk.catalog.Stage;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/** MCP tools over the service catalog (read-only). */
@Component
class CatalogTools {

    /** Compact view of a monitored service; deliberately smaller than the REST representation. */
    record ServiceSummary(long id, String name, String ownerTeam, Stage stage, ServiceStatus status) {
    }

    private final MonitoredServiceRepository services;

    CatalogTools(MonitoredServiceRepository services) {
        this.services = services;
    }

    @McpTool(name = "list_services",
            description = """
                    Lists the services monitored by opsdesk, sorted by name. Each entry has id, name, ownerTeam, \
                    stage (DEV or PROD) and status (UP, DEGRADED or DOWN). Call this first to find out which \
                    services exist and which are unhealthy, for example before opening an incident. Read-only.""",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    List<ServiceSummary> listServices(
            @McpToolParam(required = false,
                    description = "Only return services in this stage. Valid values: DEV, PROD. Omit to return all stages.")
            Stage stage) {
        var found = stage == null ? services.findAllByOrderByNameAsc() : services.findByStageOrderByNameAsc(stage);
        return found.stream()
                .map(s -> new ServiceSummary(s.id(), s.name(), s.ownerTeam(), s.stage(), s.status()))
                .toList();
    }
}
