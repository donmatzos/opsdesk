package at.donmatzos.opsdesk.mcp;

import at.donmatzos.opsdesk.IntegrationTest;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class McpSecurityTest extends IntegrationTest {

    private static final String INIT = """
            {"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}""";

    private org.springframework.test.web.servlet.client.RestTestClient.RequestBodySpec post() {
        return client().post().uri("/mcp")
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json");
    }

    @Test
    public void shouldReturn401WithoutToken() {
        post().body(INIT).exchange().expectStatus().isUnauthorized()
                .expectHeader().valueEquals("WWW-Authenticate", "Bearer");
    }

    @Test
    public void shouldReturn401WithWrongToken() {
        post().header("Authorization", "Bearer " + mcpToken() + "x").body(INIT).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    public void shouldAcceptRightToken() {
        post().header("Authorization", "Bearer " + mcpToken()).body(INIT).exchange()
                .expectStatus().isOk();
    }

    @Test
    public void shouldLeaveRestApiOpen() {
        var result = client().get().uri("/api/services").exchange().expectStatus().isOk();
        assertThat(result).isNotNull();
    }
}
