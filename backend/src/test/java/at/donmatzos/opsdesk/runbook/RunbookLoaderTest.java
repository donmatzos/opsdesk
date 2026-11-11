package at.donmatzos.opsdesk.runbook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.donmatzos.opsdesk.ai.search.Runbook;
import org.springframework.core.io.DefaultResourceLoader;
import org.testng.annotations.Test;

public class RunbookLoaderTest {

    private static RunbookLoader loader(String location) {
        return new RunbookLoader(new DefaultResourceLoader(), location);
    }

    @Test
    public void shouldLoadTheShippedRunbooks() {
        var runbooks = loader("classpath*:runbooks/*.md").load();
        assertThat(runbooks).extracting(Runbook::id).containsExactly("database-unavailable", "http-errors",
                "mcp-token-rotation", "pod-not-ready", "sev1-incident");
        assertThat(runbooks).allSatisfy(r -> {
            assertThat(r.title()).isNotBlank();
            assertThat(r.chunks().size()).isGreaterThanOrEqualTo(3);
        });
    }

    @Test
    public void shouldSkipTemplatesAndReadme() {
        assertThat(loader("classpath:runbook-fixtures/*.md").load()).extracting(Runbook::id).containsExactly("a");
    }

    @Test
    public void shouldFailWhenNoRunbookIsFound() {
        assertThatThrownBy(() -> loader("classpath:nothing-here/*.md").load())
                .isInstanceOf(IllegalStateException.class);
    }
}
