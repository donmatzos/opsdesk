package at.donmatzos.opsdesk.mcp;

import at.donmatzos.opsdesk.IntegrationTest;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class McpServerTest extends IntegrationTest {

    @LocalServerPort
    private int port;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MeterRegistry registry;

    /** The tool result is one text block holding a JSON array of services. */
    private static List<String> names(McpSchema.CallToolResult result) {
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        return JSON.readTree(text).valueStream().map(n -> n.get("name").asString()).toList();
    }

    @Test
    public void shouldListToolsAndCallListServices() {
        try (McpSyncClient mcp = McpTestClient.connect(port, mcpToken())) {
            McpSchema.Tool tool = mcp.listTools().tools().stream()
                    .filter(t -> t.name().equals("list_services")).findFirst().orElseThrow();
            assertThat(tool.description()).isNotBlank();
            assertThat(tool.annotations().readOnlyHint()).isTrue();

            McpSchema.CallToolResult all = mcp.callTool(new McpSchema.CallToolRequest("list_services", Map.of()));
            assertThat(all.isError()).isFalse();
            assertThat(names(all)).hasSize(4).contains("billing-api");

            McpSchema.CallToolResult dev = mcp.callTool(
                    new McpSchema.CallToolRequest("list_services", Map.of("stage", "DEV")));
            assertThat(dev.isError()).isFalse();
            assertThat(names(dev)).containsExactly("report-batch");
        }
    }

    @Test
    public void shouldServeIndependentClientsWithoutSessions() {
        try (McpSyncClient first = McpTestClient.connect(port, mcpToken());
             McpSyncClient second = McpTestClient.connect(port, mcpToken())) {
            assertThat(first.callTool(new McpSchema.CallToolRequest("list_services", Map.of())).isError()).isFalse();
            assertThat(second.callTool(new McpSchema.CallToolRequest("list_services", Map.of())).isError()).isFalse();
        }

        // Stateless: no initialize, no Mcp-Session-Id, and the request still works.
        client().post().uri("/mcp")
                .header("Authorization", "Bearer " + mcpToken())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body).contains("list_services"));
    }

    private static McpSchema.CallToolResult call(McpSyncClient mcp, String tool, Map<String, Object> args) {
        return mcp.callTool(new McpSchema.CallToolRequest(tool, args));
    }

    private static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().getFirst()).text();
    }

    private double opened(String severity) {
        return registry.counter("opsdesk.incidents.opened", "severity", severity).count();
    }

    @Test
    public void shouldOpenAndResolveAnIncidentThroughMcp() {
        try (McpSyncClient mcp = McpTestClient.connect(port, mcpToken())) {
            var status = call(mcp, "get_service_status", Map.of("service_name", "billing-api"));
            assertThat(status.isError()).isFalse();
            assertThat(JSON.readTree(text(status)).get("name").asString()).isEqualTo("billing-api");

            var opened = call(mcp, "open_incident",
                    Map.of("service_name", "billing-api", "severity", "SEV2", "summary", "p99 latency above 2s"));
            assertThat(opened.isError()).isFalse();
            long id = JSON.readTree(text(opened)).get("id").asLong();

            var fetched = JSON.readTree(text(call(mcp, "get_incident", Map.of("incident_id", id))));
            assertThat(fetched.get("state").asString()).isEqualTo("OPEN");
            assertThat(fetched.get("service").asString()).isEqualTo("billing-api");

            var active = JSON.readTree(text(call(mcp, "list_active_incidents", Map.of("severity", "SEV2"))));
            assertThat(active.get("incidents").valueStream().map(n -> n.get("id").asLong())).contains(id);

            var resolved = call(mcp, "change_incident_state", Map.of("incident_id", id, "state", "RESOLVED"));
            assertThat(resolved.isError()).isFalse();
            var node = JSON.readTree(text(resolved));
            assertThat(node.get("state").asString()).isEqualTo("RESOLVED");
            assertThat(node.get("resolvedAt").isNull()).isFalse();
        }
    }

    @Test
    public void shouldCountMcpWritesInMetrics() {
        double before = opened("SEV1");
        try (McpSyncClient mcp = McpTestClient.connect(port, mcpToken())) {
            var result = call(mcp, "open_incident",
                    Map.of("service_name", "billing-api", "severity", "SEV1", "summary", "metrics check"));
            assertThat(result.isError()).isFalse();
        }
        assertThat(opened("SEV1")).isEqualTo(before + 1);
    }

    @Test
    public void shouldReturnToolErrorsNotServerErrors() {
        try (McpSyncClient mcp = McpTestClient.connect(port, mcpToken())) {
            var missing = call(mcp, "get_incident", Map.of("incident_id", 999_999_999L));
            assertThat(missing.isError()).isTrue();
            assertThat(text(missing)).isEqualTo("Incident 999999999 not found");

            long id = JSON.readTree(text(call(mcp, "open_incident",
                    Map.of("service_name", "billing-api", "severity", "SEV3", "summary", "transition check"))))
                    .get("id").asLong();
            call(mcp, "change_incident_state", Map.of("incident_id", id, "state", "RESOLVED"));
            var illegal = call(mcp, "change_incident_state", Map.of("incident_id", id, "state", "OPEN"));
            assertThat(illegal.isError()).isTrue();
            assertThat(text(illegal)).isEqualTo("Cannot change incident state from RESOLVED to OPEN");

            var badSeverity = call(mcp, "open_incident",
                    Map.of("service_name", "billing-api", "severity", "SEV9", "summary", "x"));
            assertThat(badSeverity.isError()).isTrue();
            assertThat(text(badSeverity)).contains("SEV1, SEV2, SEV3");

            var unknown = call(mcp, "get_service_status", Map.of("service_name", "nope"));
            assertThat(unknown.isError()).isTrue();
            assertThat(text(unknown)).isEqualTo("Unknown service 'nope'. Call list_services to see valid names.");

            // The server still answers after errors.
            assertThat(call(mcp, "list_services", Map.of()).isError()).isFalse();
        }
    }

    @Test
    public void shouldListActiveIncidentsWithoutSeverityFilter() {
        try (McpSyncClient mcp = McpTestClient.connect(port, mcpToken())) {
            long id = JSON.readTree(text(call(mcp, "open_incident",
                    Map.of("service_name", "billing-api", "severity", "SEV3", "summary", "unfiltered list"))))
                    .get("id").asLong();

            var all = call(mcp, "list_active_incidents", Map.of());
            assertThat(all.isError()).isFalse();
            assertThat(JSON.readTree(text(all)).get("incidents").valueStream().map(n -> n.get("id").asLong()))
                    .contains(id);

            // A blank severity is treated as no filter, never a transport error.
            var blank = call(mcp, "list_active_incidents", Map.of("severity", ""));
            assertThat(blank.isError()).isFalse();
            assertThat(JSON.readTree(text(blank)).get("incidents").valueStream().map(n -> n.get("id").asLong()))
                    .contains(id);
        }
    }
}
