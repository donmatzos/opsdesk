// MOCK: Azure OpenAI + Azure AI Search HTTP calls — no Azure account; compiled and tested against a local fake server only, see docs/context/cloud-mocks.md
package at.donmatzos.opsdesk.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The one place that talks HTTP to Azure: JSON POST with the {@code api-key} header, a 30 s timeout, and the error
 * rules shared by all adapters. Errors name the service and the status code only, never the response body or a key.
 */
public final class AzureHttp {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient http;

    public AzureHttp(HttpClient http) {
        this.http = Objects.requireNonNull(http);
    }

    public static String toJson(Object value) {
        return JSON.writeValueAsString(value);
    }

    /**
     * Sends {@code body} as JSON and returns the parsed 2xx response.
     *
     * @param service name used in error messages, e.g. "Azure AI Search"
     * @throws AiBackendException on a non-2xx status, an unreachable service or an unparsable response
     */
    public JsonNode postJson(String service, URI uri, String apiKey, String body) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new AiBackendException(service + " returned HTTP " + response.statusCode());
            }
            return JSON.readTree(response.body());
        } catch (IOException e) {
            throw new AiBackendException(service + " is not reachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiBackendException(service + " call was interrupted", e);
        } catch (JacksonException _) {
            throw new AiBackendException(service + " returned an unexpected response");
        }
    }
}
