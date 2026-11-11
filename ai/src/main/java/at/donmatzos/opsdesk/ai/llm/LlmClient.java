package at.donmatzos.opsdesk.ai.llm;

import at.donmatzos.opsdesk.ai.search.RetrievedChunk;
import java.util.List;

/** Composes an answer from retrieved passages. Contract: {@code passages} is never empty. */
public interface LlmClient {

    LlmAnswer answer(String question, List<RetrievedChunk> passages);
}
