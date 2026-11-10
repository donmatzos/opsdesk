package at.donmatzos.opsdesk.mcp;

import at.donmatzos.opsdesk.IntegrationTest;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class McpServerTest extends IntegrationTest {

    @LocalServerPort
    private int port;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final String token = UUID.randomUUID().toString();

    /** The tool result is one text block holding a JSON array of services. */
    private static List<String> names(McpSchema.CallToolResult result) {
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        return JSON.readTree(text).valueStream().map(n -> n.get("name").asString()).toList();
    }

    @Test
    public void shouldListToolsAndCallListServices() {
        try (McpSyncClient mcp = McpTestClient.connect(port, token)) {
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
        try (McpSyncClient first = McpTestClient.connect(port, token);
             McpSyncClient second = McpTestClient.connect(port, token)) {
            assertThat(first.callTool(new McpSchema.CallToolRequest("list_services", Map.of())).isError()).isFalse();
            assertThat(second.callTool(new McpSchema.CallToolRequest("list_services", Map.of())).isError()).isFalse();
        }

        // Stateless: no initialize, no Mcp-Session-Id, and the request still works.
        client().post().uri("/mcp")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body).contains("list_services"));
    }
}
