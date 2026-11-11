package at.donmatzos.opsdesk.ai.search;

import java.util.Objects;

public record RetrievedChunk(RunbookChunk chunk, double score) {

    public RetrievedChunk {
        Objects.requireNonNull(chunk);
    }
}
