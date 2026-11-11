// MOCK: Azure OpenAI + Azure AI Search settings — no Azure account; compiled and tested against a local fake server only, see docs/context/cloud-mocks.md
package at.donmatzos.opsdesk.ai;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Connection settings for profile "azure". Keys only come from the environment and are never printed. */
public record AzureSettings(URI openAiEndpoint, String openAiApiKey, String openAiDeployment,
                            URI searchEndpoint, String searchApiKey, String searchIndex) {

    static final String OPENAI_ENDPOINT = "AZURE_OPENAI_ENDPOINT";
    static final String OPENAI_KEY = "AZURE_OPENAI_API_KEY";
    static final String OPENAI_DEPLOYMENT = "AZURE_OPENAI_DEPLOYMENT";
    static final String SEARCH_ENDPOINT = "AZURE_SEARCH_ENDPOINT";
    static final String SEARCH_KEY = "AZURE_SEARCH_API_KEY";
    static final String SEARCH_INDEX = "AZURE_SEARCH_INDEX";

    public static AzureSettings from(Function<String, String> env) {
        List<String> missing = new ArrayList<>();
        String openAiEndpoint = value(env, OPENAI_ENDPOINT, missing);
        String openAiKey = value(env, OPENAI_KEY, missing);
        String deployment = value(env, OPENAI_DEPLOYMENT, missing);
        String searchEndpoint = value(env, SEARCH_ENDPOINT, missing);
        String searchKey = value(env, SEARCH_KEY, missing);
        String index = value(env, SEARCH_INDEX, missing);
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Profile 'azure' needs these environment variables: " + String.join(", ", missing));
        }
        return new AzureSettings(endpoint(OPENAI_ENDPOINT, openAiEndpoint), openAiKey, deployment,
                endpoint(SEARCH_ENDPOINT, searchEndpoint), searchKey, index);
    }

    private static String value(Function<String, String> env, String name, List<String> missing) {
        String v = env.apply(name);
        if (v == null || v.isBlank()) {
            missing.add(name);
            return null;
        }
        return v.strip();
    }

    private static URI endpoint(String name, String value) {
        URI uri = URI.create(value.endsWith("/") ? value.substring(0, value.length() - 1) : value);
        boolean local = "localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
        if (!"https".equals(uri.getScheme()) && !(local && "http".equals(uri.getScheme()))) {
            throw new IllegalStateException(name + " must be an https:// URL");
        }
        return uri;
    }

    @Override
    public String toString() {
        return "AzureSettings[openAiEndpoint=" + openAiEndpoint + ", openAiApiKey=***, openAiDeployment="
                + openAiDeployment + ", searchEndpoint=" + searchEndpoint + ", searchApiKey=***, searchIndex="
                + searchIndex + "]";
    }
}
