package at.donmatzos.opsdesk.ai;

import at.donmatzos.opsdesk.ai.search.RetrievedChunk;
import java.util.List;
import java.util.Objects;

public record AssistAnswer(String answer, List<RetrievedChunk> sources, String model, boolean mock) {

    public AssistAnswer {
        Objects.requireNonNull(answer);
        sources = List.copyOf(sources);
        Objects.requireNonNull(model);
    }
}
