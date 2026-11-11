package at.donmatzos.opsdesk.ai.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

public class TokenizerTest {

    @Test
    public void shouldLowercaseSplitAndDropStopWords() {
        assertThat(Tokenizer.tokens("How to Restart the DB, a pod-x!"))
                .containsExactly("restart", "db", "pod");
    }

    @Test
    public void shouldFoldSimplePlurals() {
        assertThat(Tokenizer.tokens("pods errors bus")).containsExactly("pod", "error", "bus");
    }
}
