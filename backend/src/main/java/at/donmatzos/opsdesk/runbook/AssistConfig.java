package at.donmatzos.opsdesk.runbook;

import at.donmatzos.opsdesk.ai.RunbookAssistant;
import at.donmatzos.opsdesk.ai.llm.LlmClient;
import at.donmatzos.opsdesk.ai.search.Retriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AssistConfig {

    @Bean
    RunbookAssistant runbookAssistant(Retriever retriever, LlmClient llmClient) {
        return new RunbookAssistant(retriever, llmClient);
    }
}
