// MOCK: Azure AI Search document upload — no Azure account; compiled and tested against a local fake server only, see docs/context/cloud-mocks.md
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

/** Uploads runbook chunks with {@code mergeOrUpload}: {@code POST {endpoint}/indexes/{index}/docs/index}. */
public final class AzureAiSearchIndexer {

    private final AzureSettings settings;
    private final AzureCredentials credentials;
    private final AzureHttp http;

    public AzureAiSearchIndexer(AzureSettings settings, AzureCredentials credentials, HttpClient http) {
        this.settings = Objects.requireNonNull(settings);
        this.credentials = Objects.requireNonNull(credentials);
        this.http = new AzureHttp(http);
    }

    public void upload(List<Runbook> runbooks) {
        List<Map<String, Object>> docs = new ArrayList<>();
        for (Runbook runbook : runbooks) {
            for (RunbookChunk c : runbook.chunks()) {
                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("@search.action", "mergeOrUpload");
                doc.put("id", c.id());
                doc.put("runbook", c.runbookId());
                doc.put("title", c.title());
                doc.put("section", c.section());
                doc.put("content", c.text());
                doc.put("order", c.order());
                docs.add(doc);
            }
        }
        JsonNode results = http.postJson(AzureAiSearchRetriever.SERVICE, URI.create(settings.searchEndpoint()
                + "/indexes/" + settings.searchIndex() + "/docs/index?api-version="
                + AzureAiSearchRetriever.API_VERSION),
                credentials.searchApiKey(), AzureHttp.toJson(Map.of("value", docs))).path("value");
        // HTTP 207: some documents were rejected although the call itself succeeded.
        long rejected = results.valueStream()
                .filter(d -> d.path("status").isBoolean() && !d.path("status").asBoolean()).count();
        if (rejected > 0) {
            throw new AiBackendException(
                    AzureAiSearchRetriever.SERVICE + " rejected " + rejected + " of " + results.size() + " documents");
        }
    }
}
