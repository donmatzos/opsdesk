package at.donmatzos.opsdesk.ai.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.donmatzos.opsdesk.ai.AiBackendException;
import at.donmatzos.opsdesk.ai.AzureSettings;
import at.donmatzos.opsdesk.ai.FakeServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.UUID;
import org.testng.annotations.Test;

public class AzureOpenAiLlmClientTest {

    private static AzureSettings settings(URI endpoint, String key) {
        return new AzureSettings(endpoint, "my-deployment", endpoint, "idx");
    }

    @Test
    public void shouldSendDeploymentAndPromptsWithApiKeyHeader() throws Exception {
        String key = UUID.randomUUID().toString();
        try (var server = new FakeServer(200, "{\"choices\":[{\"message\":{\"content\":\"hello [1]\"}}]}")) {
            var client = new AzureOpenAiLlmClient(settings(server.uri(), key), FakeServer.credentials(key), HttpClient.newHttpClient());
            var answer = client.answer("why?", List.of(MockLlmClientTest.passage("S", "text")));

            var seen = server.seen.get();
            assertThat(seen.path()).isEqualTo("/openai/v1/chat/completions");
            assertThat(seen.apiKey()).isEqualTo(key);
            assertThat(seen.body()).contains("\"model\":\"my-deployment\"").contains("<question>why?</question>")
                    .contains("\"role\":\"system\"").contains("max_completion_tokens");
            assertThat(seen.body()).doesNotContain(key);
            assertThat(answer.model()).isEqualTo("my-deployment");
            assertThat(answer.mock()).isFalse();
        }
    }

    @Test
    public void shouldParseTheFirstChoice() throws Exception {
        try (var server = new FakeServer(200, "{\"choices\":[{\"message\":{\"content\":\"first\"}},"
                + "{\"message\":{\"content\":\"second\"}}]}")) {
            var client = new AzureOpenAiLlmClient(settings(server.uri(), "k"), FakeServer.credentials("k"), HttpClient.newHttpClient());
            assertThat(client.answer("q", List.of(MockLlmClientTest.passage("S", "t"))).text()).isEqualTo("first");
        }
    }

    @Test
    public void shouldNotLeakBodyOrKeyOnHttpError() throws Exception {
        String key = UUID.randomUUID().toString();
        try (var server = new FakeServer(500, "boom " + key)) {
            var client = new AzureOpenAiLlmClient(settings(server.uri(), key), FakeServer.credentials(key), HttpClient.newHttpClient());
            assertThatThrownBy(() -> client.answer("q", List.of(MockLlmClientTest.passage("S", "t"))))
                    .isInstanceOf(AiBackendException.class)
                    .hasMessage("Azure OpenAI returned HTTP 500")
                    .hasMessageNotContaining(key);
        }
    }

    @Test
    public void shouldFailCleanlyOnUnparsableBody() throws Exception {
        try (var server = new FakeServer(200, "not json")) {
            var client = new AzureOpenAiLlmClient(settings(server.uri(), "k"), FakeServer.credentials("k"), HttpClient.newHttpClient());
            assertThatThrownBy(() -> client.answer("q", List.of(MockLlmClientTest.passage("S", "t"))))
                    .isInstanceOf(AiBackendException.class);
        }
    }
}
