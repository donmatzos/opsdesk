package at.donmatzos.opsdesk.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.UUID;
import org.testng.annotations.Test;

public class AzureHttpTest {

    private final AzureHttp http = new AzureHttp(HttpClient.newHttpClient());

    @Test
    public void shouldPostJsonWithApiKeyHeaderAndParseTheAnswer() throws Exception {
        String key = UUID.randomUUID().toString();
        try (var server = new FakeServer(200, "{\"a\":1}")) {
            var result = http.postJson("Svc", server.uri().resolve("/x"), key, "{\"q\":2}");
            assertThat(result.get("a").asInt()).isEqualTo(1);
            assertThat(server.seen.get().method()).isEqualTo("POST");
            assertThat(server.seen.get().apiKey()).isEqualTo(key);
            assertThat(server.seen.get().body()).isEqualTo("{\"q\":2}");
        }
    }

    @Test
    public void shouldNameServiceAndStatusButNeverBodyOrKey() throws Exception {
        String key = UUID.randomUUID().toString();
        try (var server = new FakeServer(429, "limit " + key)) {
            URI uri = server.uri();
            assertThatThrownBy(() -> http.postJson("Svc", uri, key, "{}"))
                    .isInstanceOf(AiBackendException.class).hasMessage("Svc returned HTTP 429");
        }
    }

    @Test
    public void shouldMapUnparsableBodiesAndUnreachableHosts() throws Exception {
        try (var server = new FakeServer(200, "not json")) {
            URI uri = server.uri();
            assertThatThrownBy(() -> http.postJson("Svc", uri, "k", "{}"))
                    .isInstanceOf(AiBackendException.class).hasMessage("Svc returned an unexpected response");
        }
        URI nothingListens = URI.create("http://127.0.0.1:1");
        assertThatThrownBy(() -> http.postJson("Svc", nothingListens, "k", "{}"))
                .isInstanceOf(AiBackendException.class).hasMessage("Svc is not reachable");
    }
}
