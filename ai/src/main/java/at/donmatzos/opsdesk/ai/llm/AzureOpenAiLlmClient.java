// MOCK: Azure OpenAI chat completions — no Azure account; compiled and tested against a local fake server only, see docs/context/cloud-mocks.md
package at.donmatzos.opsdesk.ai.llm;

import at.donmatzos.opsdesk.ai.AiBackendException;
import at.donmatzos.opsdesk.ai.AzureSettings;
import at.donmatzos.opsdesk.ai.search.RetrievedChunk;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Azure OpenAI v1 REST: {@code POST {endpoint}/openai/v1/chat/completions}. */
public final class AzureOpenAiLlmClient implements LlmClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final AzureSettings settings;
    private final HttpClient http;

    public AzureOpenAiLlmClient(AzureSettings settings, HttpClient http) {
        this.settings = Objects.requireNonNull(settings);
        this.http = Objects.requireNonNull(http);
    }

    @Override
    public LlmAnswer answer(String question, List<RetrievedChunk> passages) {
        String body = JSON.writeValueAsString(Map.of(
                "model", settings.openAiDeployment(),
                "messages", List.of(
                        Map.of("role", "system", "content", PromptBuilder.systemPrompt()),
                        Map.of("role", "user", "content", PromptBuilder.userPrompt(question, passages))),
                "temperature", 0.1,
                "max_completion_tokens", 600));
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(settings.openAiEndpoint() + "/openai/v1/chat/completions"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("api-key", settings.openAiApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new AiBackendException("Azure OpenAI returned HTTP " + response.statusCode());
            }
            JsonNode content = JSON.readTree(response.body()).path("choices").path(0).path("message").path("content");
            if (!content.isString()) {
                throw new AiBackendException("Azure OpenAI returned an unexpected response");
            }
            return new LlmAnswer(content.asString(), settings.openAiDeployment(), false);
        } catch (IOException e) {
            throw new AiBackendException("Azure OpenAI is not reachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiBackendException("Azure OpenAI call was interrupted", e);
        } catch (tools.jackson.core.JacksonException e) {
            throw new AiBackendException("Azure OpenAI returned an unexpected response");
        }
    }
}
