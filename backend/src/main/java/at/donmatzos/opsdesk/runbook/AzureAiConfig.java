// MOCK: Azure OpenAI + Azure AI Search wiring (profile "azure", off by default) — no Azure account; compiled and tested against a local fake server only, see docs/context/cloud-mocks.md
package at.donmatzos.opsdesk.runbook;

import at.donmatzos.opsdesk.ai.AiBackendException;
import at.donmatzos.opsdesk.ai.AzureCredentials;
import at.donmatzos.opsdesk.ai.AzureSettings;
import at.donmatzos.opsdesk.ai.llm.AzureOpenAiLlmClient;
import at.donmatzos.opsdesk.ai.llm.LlmClient;
import at.donmatzos.opsdesk.ai.search.AzureAiSearchIndexer;
import at.donmatzos.opsdesk.ai.search.AzureAiSearchRetriever;
import at.donmatzos.opsdesk.ai.search.Retriever;
import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * Profile "azure": reads six environment variables (see {@link AzureSettings}) and stops the start with one message
 * naming all missing ones. No HTTP call happens at startup unless {@code opsdesk.ai.azure.search.index-on-startup}.
 */
@Configuration
@Profile("azure")
class AzureAiConfig {

    private static final Logger log = LoggerFactory.getLogger(AzureAiConfig.class);

    @Bean
    AzureSettings azureSettings(Environment environment) {
        return AzureSettings.from(environment::getProperty);
    }

    /** Keys stay in the environment (Secret in a cluster); this bean only knows where to look them up. */
    @Bean
    AzureCredentials azureCredentials(Environment environment) {
        return AzureCredentials.fromEnvironment(environment::getProperty);
    }

    @Bean
    HttpClient azureHttpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Bean
    Retriever retriever(AzureSettings settings, AzureCredentials credentials, HttpClient azureHttpClient) {
        return new AzureAiSearchRetriever(settings, credentials, azureHttpClient);
    }

    @Bean
    LlmClient llmClient(AzureSettings settings, AzureCredentials credentials, HttpClient azureHttpClient) {
        return new AzureOpenAiLlmClient(settings, credentials, azureHttpClient);
    }

    @Bean
    @ConditionalOnBooleanProperty("opsdesk.ai.azure.search.index-on-startup")
    ApplicationRunner uploadRunbooks(AzureSettings settings, AzureCredentials credentials, HttpClient azureHttpClient,
                                     RunbookLoader loader) {
        return args -> {
            try {
                new AzureAiSearchIndexer(settings, credentials, azureHttpClient).upload(loader.load());
            } catch (AiBackendException e) {
                // A search outage or a missing index must not keep the whole incident desk from starting.
                log.warn("Runbook upload to Azure AI Search failed, the index was not updated: {}", e.getMessage());
            }
        };
    }
}
