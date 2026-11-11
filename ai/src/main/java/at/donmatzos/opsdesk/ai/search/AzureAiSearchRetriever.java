// MOCK: Azure AI Search query — no Azure account; compiled and tested against a local fake server only, see docs/context/cloud-mocks.md
package at.donmatzos.opsdesk.ai.search;

import at.donmatzos.opsdesk.ai.AiBackendException;
import at.donmatzos.opsdesk.ai.AzureCredentials;
import at.donmatzos.opsdesk.ai.AzureSettings;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Azure AI Search REST (api-version 2024-07-01): {@code POST {endpoint}/indexes/{index}/docs/search}. */
public final class AzureAiSearchRetriever implements Retriever {

    static final String API_VERSION = "2024-07-01";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final AzureSettings settings;
    private final AzureCredentials credentials;
    private final HttpClient http;

    public AzureAiSearchRetriever(AzureSettings settings, AzureCredentials credentials, HttpClient http) {
        this.settings = Objects.requireNonNull(settings);
        this.credentials = Objects.requireNonNull(credentials);
        this.http = Objects.requireNonNull(http);
    }

    @Override
    public List<RetrievedChunk> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("search", query);
        body.put("top", Math.clamp(limit, 1, 10));
        body.put("queryType", "simple");
        body.put("select", "id,runbook,title,section,content,order");
        HttpRequest request = HttpRequest.newBuilder(URI.create(settings.searchEndpoint() + "/indexes/"
                        + settings.searchIndex() + "/docs/search?api-version=" + API_VERSION))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("api-key", credentials.searchApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new AiBackendException("Azure AI Search returned HTTP " + response.statusCode());
            }
            List<RetrievedChunk> hits = new ArrayList<>();
            for (JsonNode doc : JSON.readTree(response.body()).path("value")) {
                if (doc.path("id").asString().isBlank() || doc.path("runbook").asString().isBlank()) {
                    throw new AiBackendException("Azure AI Search returned an unexpected response");
                }
                RunbookChunk chunk = new RunbookChunk(doc.path("id").asString(), doc.path("runbook").asString(),
                        doc.path("title").asString(), doc.path("section").asString(),
                        doc.path("content").asString(), doc.path("order").asInt(0));
                hits.add(new RetrievedChunk(chunk, doc.path("@search.score").asDouble(0)));
            }
            return List.copyOf(hits);
        } catch (IOException e) {
            throw new AiBackendException("Azure AI Search is not reachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiBackendException("Azure AI Search call was interrupted", e);
        } catch (JacksonException e) {
            throw new AiBackendException("Azure AI Search returned an unexpected response");
        }
    }
}
