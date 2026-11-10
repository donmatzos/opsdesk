package at.donmatzos.opsdesk.mcp;

import at.donmatzos.opsdesk.TestcontainersConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testng.annotations.Test;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/** Own context with an empty token (the default): MCP is off, the rest of the app is not affected. */
@SpringBootTest(webEnvironment = RANDOM_PORT,
        properties = {"management.server.port=0", "opsdesk.mcp.token="})
@Import(TestcontainersConfig.class)
@ActiveProfiles("demo")
public class McpDisabledTest extends AbstractTestNGSpringContextTests {

    @LocalServerPort
    private int port;

    private RestTestClient client() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    public void shouldAnswer503ForMcp() {
        client().post().uri("/mcp")
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer anything")
                .body("{}").exchange().expectStatus().isEqualTo(503);
    }

    @Test
    public void shouldKeepRestApiWorking() {
        client().get().uri("/api/services").exchange().expectStatus().isOk();
    }
}
