// MOCK: Azure AI Search document upload — no Azure account; compiled and tested against a local fake server only, see docs/context/cloud-mocks.md
package at.donmatzos.opsdesk.ai.search;

import at.donmatzos.opsdesk.ai.AiBackendException;
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
import tools.jackson.databind.json.JsonMapper;

/** Uploads runbook chunks with {@code mergeOrUpload}: {@code POST {endpoint}/indexes/{index}/docs/index}. */
public final class AzureAiSearchIndexer {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final AzureSettings settings;
    private final HttpClient http;

    public AzureAiSearchIndexer(AzureSettings settings, HttpClient http) {
        this.settings = Objects.requireNonNull(settings);
        this.http = Objects.requireNonNull(http);
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
        HttpRequest request = HttpRequest.newBuilder(URI.create(settings.searchEndpoint() + "/indexes/"
                        + settings.searchIndex() + "/docs/index?api-version=" + AzureAiSearchRetriever.API_VERSION))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("api-key", settings.searchApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("value", docs))))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new AiBackendException("Azure AI Search returned HTTP " + response.statusCode());
            }
        } catch (IOException e) {
            throw new AiBackendException("Azure AI Search is not reachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiBackendException("Azure AI Search call was interrupted", e);
        }
    }
}
