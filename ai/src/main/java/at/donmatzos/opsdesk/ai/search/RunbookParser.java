package at.donmatzos.opsdesk.ai.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Splits a runbook in Markdown into one chunk per second-level section. */
public final class RunbookParser {

    private RunbookParser() {
    }

    public static Runbook parse(String id, String markdown) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(markdown);
        String title = id;
        boolean titleSeen = false;
        boolean inFence = false;
        String section = "Overview";
        StringBuilder body = new StringBuilder();
        List<String[]> raw = new ArrayList<>();
        for (String line : markdown.replace("\r\n", "\n").split("\n", -1)) {
            if (line.stripLeading().startsWith("```")) {
                inFence = !inFence;
            }
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
        for (String[] r : raw) {
            if (r[0].equals("Overview") && r[1].isBlank()) {
                continue;
            }
            chunks.add(new RunbookChunk(RunbookChunk.idFor(id, r[0]), id, title, r[0], r[1], chunks.size()));
        }
        return new Runbook(id, title, chunks);
    }
}
