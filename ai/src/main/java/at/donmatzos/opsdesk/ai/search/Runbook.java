package at.donmatzos.opsdesk.ai.search;

import java.util.List;
import java.util.Objects;

public record Runbook(String id, String title, List<RunbookChunk> chunks) {

    public Runbook {
        Objects.requireNonNull(id);
        Objects.requireNonNull(title);
        chunks = List.copyOf(chunks);
    }
}
