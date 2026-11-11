package at.donmatzos.opsdesk.ai.search;

import java.util.Locale;
import java.util.Objects;

/** One section of a runbook; the unit that is indexed, retrieved and cited. */
public record RunbookChunk(String id, String runbookId, String title, String section, String text, int order) {

    public RunbookChunk {
        Objects.requireNonNull(id);
        Objects.requireNonNull(runbookId);
        Objects.requireNonNull(title);
        Objects.requireNonNull(section);
        Objects.requireNonNull(text);
    }

    public static String idFor(String runbookId, String section) {
        String slug = section.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return runbookId + "--" + (slug.isEmpty() ? "section" : slug);
    }

    public String path() {
        return "docs/runbooks/" + runbookId + ".md";
    }
}
