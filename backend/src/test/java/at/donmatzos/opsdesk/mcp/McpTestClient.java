package at.donmatzos.opsdesk.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import java.net.http.HttpRequest;
import java.time.Duration;

/** Builds an initialized synchronous MCP client against the Streamable HTTP endpoint of a running test server. */
public final class McpTestClient {

    private McpTestClient() {
    }

    /** The token is sent as a bearer header; the server ignores it until the token filter exists. */
    public static McpSyncClient connect(int port, String token) {
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                .endpoint("/mcp")
                .requestBuilder(HttpRequest.newBuilder().header("Authorization", "Bearer " + token))
                .build();
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .build();
        client.initialize();
        return client;
    }
}
