package at.donmatzos.opsdesk.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import at.donmatzos.opsdesk.ai.llm.LlmAnswer;
import at.donmatzos.opsdesk.ai.llm.LlmClient;
import at.donmatzos.opsdesk.ai.search.RetrievedChunk;
import at.donmatzos.opsdesk.ai.search.Retriever;
import at.donmatzos.opsdesk.ai.search.RunbookChunk;
import java.util.List;
import org.testng.annotations.Test;

public class RunbookAssistantTest {

    private final Retriever retriever = mock(Retriever.class);
    private final LlmClient llm = mock(LlmClient.class);
    private final RunbookAssistant assistant = new RunbookAssistant(retriever, llm);

    @Test
    public void shouldNotCallTheModelWhenNoRunbookMatches() {
        when(retriever.search(anyString(), anyInt())).thenReturn(List.of());
        var answer = assistant.ask("xyzzy");
        assertThat(answer.answer()).isEqualTo("No runbook covers this question yet.");
        assertThat(answer.sources()).isEmpty();
        assertThat(answer.model()).isEqualTo("none");
        assertThat(answer.mock()).isFalse();
        verifyNoInteractions(llm);
    }

    @Test
    public void shouldPassTopThreePassagesToTheModel() {
        var hits = List.of(new RetrievedChunk(new RunbookChunk("a--s", "a", "A", "S", "t", 0), 2.0));
        when(retriever.search("q", 3)).thenReturn(hits);
        when(llm.answer(eq("q"), anyList())).thenReturn(new LlmAnswer("text [1]", "m", true));
        var answer = assistant.ask("q");
        verify(retriever).search("q", 3);
        verify(llm).answer("q", hits);
        assertThat(answer.answer()).isEqualTo("text [1]");
        assertThat(answer.sources()).isEqualTo(hits);
        assertThat(answer.model()).isEqualTo("m");
        assertThat(answer.mock()).isTrue();
    }
}
