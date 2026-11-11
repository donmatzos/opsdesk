package at.donmatzos.opsdesk.ai.llm;

import static org.assertj.core.api.Assertions.assertThat;

import at.donmatzos.opsdesk.ai.search.RetrievedChunk;
import at.donmatzos.opsdesk.ai.search.RunbookChunk;
import java.util.List;
import org.testng.annotations.Test;

public class MockLlmClientTest {

    static RetrievedChunk passage(String section, String text) {
        return new RetrievedChunk(new RunbookChunk("rb--" + section, "rb", "Runbook", section, text, 0), 1.0);
    }

    @Test
    public void shouldMarkMockAnswersAndCiteEveryPassage() {
        var answer = new MockLlmClient().answer("q", List.of(
                passage("First", "One. Two. Three."), passage("Second", "- a\n- b\n- c\n- d")));
        assertThat(answer.mock()).isTrue();
        assertThat(answer.model()).isEqualTo("mock");
        assertThat(answer.text()).startsWith("MOCK answer (no language model was called).")
                .contains("[1] Runbook / First: One. Two.")
                .doesNotContain("Three.")
                .contains("[2] Runbook / Second:")
                .doesNotContain("- d");
    }
}
