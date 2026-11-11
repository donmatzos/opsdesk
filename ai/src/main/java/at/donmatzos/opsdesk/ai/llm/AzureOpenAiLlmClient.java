// MOCK: Azure OpenAI chat completions — no Azure account; compiled and tested against a local fake server only, see docs/context/cloud-mocks.md
package at.donmatzos.opsdesk.ai.llm;

import at.donmatzos.opsdesk.ai.AiBackendException;
import at.donmatzos.opsdesk.ai.AzureCredentials;
import at.donmatzos.opsdesk.ai.AzureHttp;
import at.donmatzos.opsdesk.ai.AzureSettings;
import at.donmatzos.opsdesk.ai.search.RetrievedChunk;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.databind.JsonNode;

/** Azure OpenAI v1 REST: {@code POST {endpoint}/openai/v1/chat/completions}. */
public final class AzureOpenAiLlmClient implements LlmClient {

    private static final String SERVICE = "Azure OpenAI";
    private static final String CONTENT = "content";

    private final AzureSettings settings;
    private final AzureCredentials credentials;
    private final AzureHttp http;

    public AzureOpenAiLlmClient(AzureSettings settings, AzureCredentials credentials, HttpClient http) {
        this.settings = Objects.requireNonNull(settings);
        this.credentials = Objects.requireNonNull(credentials);
        this.http = new AzureHttp(http);
    }

    @Override
    public LlmAnswer answer(String question, List<RetrievedChunk> passages) {
        String body = AzureHttp.toJson(Map.of(
                "model", settings.openAiDeployment(),
                "messages", List.of(
                        Map.of("role", "system", CONTENT, PromptBuilder.systemPrompt()),
                        Map.of("role", "user", CONTENT, PromptBuilder.userPrompt(question, passages))),
                "temperature", 0.1,
                "max_completion_tokens", 600));
        JsonNode response = http.postJson(SERVICE,
                URI.create(settings.openAiEndpoint() + "/openai/v1/chat/completions"),
                credentials.openAiApiKey(), body);
        JsonNode content = response.path("choices").path(0).path("message").path(CONTENT);
        if (!content.isString()) {
            throw new AiBackendException(SERVICE + " returned an unexpected response");
        }
        return new LlmAnswer(content.asString(), settings.openAiDeployment(), false);
    }
}
