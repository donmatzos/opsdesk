// MOCK: Azure AI Search query — no Azure account; compiled and tested against a local fake server only, see docs/context/cloud-mocks.md
package at.donmatzos.opsdesk.ai.search;

import at.donmatzos.opsdesk.ai.AiBackendException;
import at.donmatzos.opsdesk.ai.AzureCredentials;
import at.donmatzos.opsdesk.ai.AzureHttp;
import at.donmatzos.opsdesk.ai.AzureSettings;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.databind.JsonNode;

/** Azure AI Search REST (api-version 2024-07-01): {@code POST {endpoint}/indexes/{index}/docs/search}. */
public final class AzureAiSearchRetriever implements Retriever {

    static final String API_VERSION = "2024-07-01";
    static final String SERVICE = "Azure AI Search";

    private final AzureSettings settings;
    private final AzureCredentials credentials;
    private final AzureHttp http;

    public AzureAiSearchRetriever(AzureSettings settings, AzureCredentials credentials, HttpClient http) {
        this.settings = Objects.requireNonNull(settings);
        this.credentials = Objects.requireNonNull(credentials);
        this.http = new AzureHttp(http);
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
        JsonNode response = http.postJson(SERVICE, URI.create(settings.searchEndpoint() + "/indexes/"
                + settings.searchIndex() + "/docs/search?api-version=" + API_VERSION),
                credentials.searchApiKey(), AzureHttp.toJson(body));
        List<RetrievedChunk> hits = new ArrayList<>();
        for (JsonNode doc : response.path("value")) {
            if (doc.path("id").asString().isBlank() || doc.path("runbook").asString().isBlank()) {
                throw new AiBackendException(SERVICE + " returned an unexpected response");
            }
            RunbookChunk chunk = new RunbookChunk(doc.path("id").asString(), doc.path("runbook").asString(),
                    doc.path("title").asString(), doc.path("section").asString(),
                    doc.path("content").asString(), doc.path("order").asInt(0));
            hits.add(new RetrievedChunk(chunk, doc.path("@search.score").asDouble(0)));
        }
        return List.copyOf(hits);
    }
}
