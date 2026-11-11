package at.donmatzos.opsdesk.ai;

import at.donmatzos.opsdesk.ai.llm.LlmAnswer;
import at.donmatzos.opsdesk.ai.llm.LlmClient;
import at.donmatzos.opsdesk.ai.search.RetrievedChunk;
import at.donmatzos.opsdesk.ai.search.Retriever;
import java.util.List;
import java.util.Objects;

/** Grounded answers: retrieve runbook sections first, and only then (and only with hits) ask the model. */
public final class RunbookAssistant {

    public static final String NO_MATCH = "No runbook covers this question yet.";
    static final int TOP = 3;

    private final Retriever retriever;
    private final LlmClient llm;

    public RunbookAssistant(Retriever retriever, LlmClient llm) {
        this.retriever = Objects.requireNonNull(retriever);
        this.llm = Objects.requireNonNull(llm);
    }

    public AssistAnswer ask(String question) {
        Objects.requireNonNull(question);
        List<RetrievedChunk> hits = retriever.search(question, TOP);
        if (hits.isEmpty()) {
            return new AssistAnswer(NO_MATCH, List.of(), "none", false);
        }
        LlmAnswer answer = llm.answer(question, hits);
        return new AssistAnswer(answer.text(), hits, answer.model(), answer.mock());
    }
}
