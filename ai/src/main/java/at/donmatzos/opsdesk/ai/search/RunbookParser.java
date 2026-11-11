package at.donmatzos.opsdesk.ai.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Splits a runbook in Markdown into one chunk per second-level section. */
public final class RunbookParser {

    private RunbookParser() {
    }

    /** Tracks code fences (``` or ~~~): returns the open marker, or null outside a fence. */
    private static String nextFenceState(String open, String line) {
        String t = line.stripLeading();
        String marker = t.startsWith("```") ? "```" : t.startsWith("~~~") ? "~~~" : null;
        if (marker == null) {
            return open;
        }
        if (open != null) {
            return open.equals(marker) ? null : open;
        }
        // A fence that closes on its own line (```code```) opens nothing.
        return t.indexOf(marker, 3) >= 0 ? null : marker;
    }

    public static Runbook parse(String id, String markdown) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(markdown);
        String title = id;
        boolean titleSeen = false;
        String fence = null;
        String section = "Overview";
        StringBuilder body = new StringBuilder();
        List<String[]> raw = new ArrayList<>();
        for (String line : markdown.replace("\r\n", "\n").split("\n", -1)) {
            fence = nextFenceState(fence, line);
            boolean inFence = fence != null;
            if (!inFence && !titleSeen && line.startsWith("# ")) {
                title = line.substring(2).strip();
                titleSeen = true;
            } else if (!inFence && line.startsWith("## ")) {
                raw.add(new String[] {section, body.toString().strip()});
                section = line.substring(3).strip();
                body.setLength(0);
            } else {
                body.append(line).append('\n');
            }
        }
        raw.add(new String[] {section, body.toString().strip()});
        List<RunbookChunk> chunks = new ArrayList<>();
        Map<String, Integer> seen = new HashMap<>();
        for (String[] r : raw) {
            if (r[0].equals("Overview") && r[1].isBlank()) {
                continue;
            }
            String base = RunbookChunk.idFor(id, r[0]);
            int n = seen.merge(base, 1, Integer::sum);
            chunks.add(new RunbookChunk(n == 1 ? base : base + "-" + n, id, title, r[0], r[1], chunks.size()));
        }
        return new Runbook(id, title, chunks);
    }
}
