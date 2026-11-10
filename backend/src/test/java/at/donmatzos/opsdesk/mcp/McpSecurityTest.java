package at.donmatzos.opsdesk.mcp;

import at.donmatzos.opsdesk.IntegrationTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class McpSecurityTest extends IntegrationTest {

    @LocalServerPort
    private int port;

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

    private HttpResponse<String> rawPost(String path, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(INIT));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Real server port, no MockMvc: path variants must not reach the MCP handler without the token. */
    @Test
    public void shouldNeverServeMcpWithoutTokenOnPathVariants() throws Exception {
        for (String path : new String[] {"/mcp/", "/mcp;a=b", "//mcp", "/MCP", "/mcp.json"}) {
            var anonymous = rawPost(path, null);
            assertThat(anonymous.statusCode()).as("no token: " + path).isIn(401, 404, 405);
            assertThat(anonymous.body()).as("no token: " + path).doesNotContain("list_services");

            var wrong = rawPost(path, mcpToken() + "x");
            assertThat(wrong.statusCode()).as("wrong token: " + path).isIn(401, 404, 405);
            assertThat(wrong.body()).as("wrong token: " + path).doesNotContain("list_services");

            var authed = rawPost(path, mcpToken());
            assertThat(authed.statusCode()).as("valid token: " + path).isLessThan(500);
        }
    }
}
