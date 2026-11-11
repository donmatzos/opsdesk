package at.donmatzos.opsdesk.runbook;

import static org.assertj.core.api.Assertions.assertThat;

import at.donmatzos.opsdesk.ai.AzureSettings;
import at.donmatzos.opsdesk.ai.llm.AzureOpenAiLlmClient;
import at.donmatzos.opsdesk.ai.llm.LlmClient;
import at.donmatzos.opsdesk.ai.llm.MockLlmClient;
import at.donmatzos.opsdesk.ai.search.AzureAiSearchRetriever;
import at.donmatzos.opsdesk.ai.search.Retriever;
import at.donmatzos.opsdesk.ai.search.RunbookIndex;
import java.util.UUID;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testng.annotations.Test;

/** Review Focus 1: profile "azure" needs its variables; without the profile no Azure bean exists. */
public class AiProfileWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(RunbookLoader.class, AssistConfig.class, LocalAiConfig.class, AzureAiConfig.class)
            .withPropertyValues("opsdesk.ai.runbooks.location=classpath*:runbooks/*.md");

    @Test
    public void shouldUseMocksWithoutProfileAzure() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(Retriever.class)).isInstanceOf(RunbookIndex.class);
            assertThat(context.getBean(LlmClient.class)).isInstanceOf(MockLlmClient.class);
            assertThat(context).doesNotHaveBean(AzureSettings.class);
        });
    }

    @Test
    public void shouldStopTheStartWithProfileAzureAndNoEnvironment() {
        runner.withInitializer(c -> c.getEnvironment().setActiveProfiles("azure")).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("AZURE_OPENAI_ENDPOINT")
                    .hasMessageContaining("AZURE_OPENAI_API_KEY").hasMessageContaining("AZURE_OPENAI_DEPLOYMENT")
                    .hasMessageContaining("AZURE_SEARCH_ENDPOINT").hasMessageContaining("AZURE_SEARCH_API_KEY")
                    .hasMessageContaining("AZURE_SEARCH_INDEX");
        });
    }

    @Test
    public void shouldUseAzureAdaptersWhenConfigured() {
        runner.withInitializer(c -> c.getEnvironment().setActiveProfiles("azure"))
                .withPropertyValues("AZURE_OPENAI_ENDPOINT=http://localhost:1",
                        "AZURE_OPENAI_API_KEY=" + UUID.randomUUID(), "AZURE_OPENAI_DEPLOYMENT=chat",
                        "AZURE_SEARCH_ENDPOINT=http://localhost:1", "AZURE_SEARCH_API_KEY=" + UUID.randomUUID(),
                        "AZURE_SEARCH_INDEX=idx")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(Retriever.class)).isInstanceOf(AzureAiSearchRetriever.class);
                    assertThat(context.getBean(LlmClient.class)).isInstanceOf(AzureOpenAiLlmClient.class);
                });
    }
}
