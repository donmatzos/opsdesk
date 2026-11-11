// MOCK: Azure OpenAI + Azure AI Search wiring (profile "azure", off by default) — no Azure account; compiled and tested against a local fake server only, see docs/context/cloud-mocks.md
package at.donmatzos.opsdesk.runbook;

import at.donmatzos.opsdesk.ai.AzureSettings;
import at.donmatzos.opsdesk.ai.llm.AzureOpenAiLlmClient;
import at.donmatzos.opsdesk.ai.llm.LlmClient;
import at.donmatzos.opsdesk.ai.search.AzureAiSearchIndexer;
import at.donmatzos.opsdesk.ai.search.AzureAiSearchRetriever;
import at.donmatzos.opsdesk.ai.search.Retriever;
import java.net.http.HttpClient;
import java.time.Duration;
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

    @Bean
    AzureSettings azureSettings(Environment environment) {
        return AzureSettings.from(environment::getProperty);
    }

    @Bean
    HttpClient azureHttpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Bean
    Retriever retriever(AzureSettings settings, HttpClient azureHttpClient) {
        return new AzureAiSearchRetriever(settings, azureHttpClient);
    }

    @Bean
    LlmClient llmClient(AzureSettings settings, HttpClient azureHttpClient) {
        return new AzureOpenAiLlmClient(settings, azureHttpClient);
    }

    @Bean
    @ConditionalOnBooleanProperty("opsdesk.ai.azure.search.index-on-startup")
    ApplicationRunner uploadRunbooks(AzureSettings settings, HttpClient azureHttpClient, RunbookLoader loader) {
        return args -> new AzureAiSearchIndexer(settings, azureHttpClient).upload(loader.load());
    }
}
