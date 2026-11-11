package at.donmatzos.opsdesk.runbook;

import at.donmatzos.opsdesk.ai.llm.LlmClient;
import at.donmatzos.opsdesk.ai.llm.MockLlmClient;
import at.donmatzos.opsdesk.ai.search.Retriever;
import at.donmatzos.opsdesk.ai.search.RunbookIndex;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * MOCK: the default (every profile except "azure"). BM25 over the packaged runbooks and a mock model instead of
 * Azure AI Search and Azure OpenAI — no Azure account; see docs/context/cloud-mocks.md.
 */
@Configuration
@Profile("!azure")
class LocalAiConfig {

    @Bean
    Retriever retriever(RunbookLoader loader) {
        return new RunbookIndex(loader.load());
    }

    @Bean
    LlmClient llmClient() {
        return new MockLlmClient();
    }
}
