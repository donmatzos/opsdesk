package at.donmatzos.opsdesk.ai.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.donmatzos.opsdesk.ai.AiBackendException;
import at.donmatzos.opsdesk.ai.AzureSettings;
import at.donmatzos.opsdesk.ai.FakeServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.UUID;
import org.testng.annotations.Test;

public class AzureAiSearchRetrieverTest {

    private static AzureSettings settings(URI endpoint, String key) {
        return new AzureSettings(endpoint, key, "d", endpoint, key, "my-index");
    }

    @Test
    public void shouldQueryTheIndexAndMapScores() throws Exception {
        String key = UUID.randomUUID().toString();
        String reply = "{\"value\":[{\"@search.score\":2.5,\"id\":\"a--s\",\"runbook\":\"a\",\"title\":\"A\","
                + "\"section\":\"S\",\"content\":\"text\",\"order\":1}]}";
        try (var server = new FakeServer(200, reply)) {
            var hits = new AzureAiSearchRetriever(settings(server.uri(), key), HttpClient.newHttpClient())
                    .search("pod", 2);

            var seen = server.seen.get();
            assertThat(seen.path()).isEqualTo("/indexes/my-index/docs/search?api-version=2024-07-01");
            assertThat(seen.apiKey()).isEqualTo(key);
            assertThat(seen.body()).contains("\"search\":\"pod\"").contains("\"top\":2");
            assertThat(hits).hasSize(1);
            assertThat(hits.get(0).score()).isEqualTo(2.5);
            assertThat(hits.get(0).chunk().id()).isEqualTo("a--s");
            assertThat(hits.get(0).chunk().order()).isEqualTo(1);
        }
    }

    @Test
    public void shouldNotLeakBodyOrKeyOnHttpError() throws Exception {
        String key = UUID.randomUUID().toString();
        try (var server = new FakeServer(403, key)) {
            var retriever = new AzureAiSearchRetriever(settings(server.uri(), key), HttpClient.newHttpClient());
            assertThatThrownBy(() -> retriever.search("x", 1)).isInstanceOf(AiBackendException.class)
                    .hasMessage("Azure AI Search returned HTTP 403");
        }
    }

    @Test
    public void shouldNotCallAzureForBlankQueries() throws Exception {
        try (var server = new FakeServer(200, "{}")) {
            assertThat(new AzureAiSearchRetriever(settings(server.uri(), "k"), HttpClient.newHttpClient())
                    .search(" ", 1)).isEmpty();
            assertThat(server.seen.get()).isNull();
        }
    }
}
