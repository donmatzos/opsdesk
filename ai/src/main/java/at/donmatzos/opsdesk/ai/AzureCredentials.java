// MOCK: Azure OpenAI + Azure AI Search credentials — no Azure account; compiled and tested against a local fake server only, see docs/context/cloud-mocks.md
package at.donmatzos.opsdesk.ai;

import java.util.Objects;
import java.util.function.Function;

/**
 * Where the API keys come from. The keys live in the environment (locally {@code deploy/compose/.env}, in a cluster a
 * Secret) and are looked up when a request is built; no class holds a copy in a field, and none prints them.
 */
public interface AzureCredentials {

    String openAiApiKey();

    String searchApiKey();

    /** Reads {@code AZURE_OPENAI_API_KEY} and {@code AZURE_SEARCH_API_KEY} from {@code env} on every call. */
    static AzureCredentials fromEnvironment(Function<String, String> env) {
        Objects.requireNonNull(env);
        return new AzureCredentials() {
            @Override
            public String openAiApiKey() {
                return required(env, AzureSettings.OPENAI_KEY);
            }

            @Override
            public String searchApiKey() {
                return required(env, AzureSettings.SEARCH_KEY);
            }

            @Override
            public String toString() {
                return "AzureCredentials[environment, keys not shown]";
            }
        };
    }

    private static String required(Function<String, String> env, String name) {
        String value = env.apply(name);
        if (value == null || value.isBlank()) {
            throw new AiBackendException(name + " is not set");
        }
        return value.strip();
    }
}
