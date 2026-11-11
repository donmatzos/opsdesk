package at.donmatzos.opsdesk.ai.llm;

import at.donmatzos.opsdesk.ai.search.RetrievedChunk;
import java.util.Arrays;
import java.util.List;

/**
 * MOCK: stands in for Azure OpenAI — no language model is called. It lists the start of each retrieved section so the
 * flow (retrieval, sources, citations) can be shown without an account, see docs/context/cloud-mocks.md.
 */
public final class MockLlmClient implements LlmClient {

    static final String PREFIX = "MOCK answer (no language model was called). The most relevant runbook sections:";
    private static final int MAX_EXCERPT = 300;

    @Override
    public LlmAnswer answer(String question, List<RetrievedChunk> passages) {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < passages.size(); i++) {
            var chunk = passages.get(i).chunk();
            sb.append('\n').append('[').append(i + 1).append("] ").append(chunk.title()).append(" / ")
                    .append(chunk.section()).append(": ").append(excerpt(chunk.text()));
        }
        return new LlmAnswer(sb.toString(), "mock", true);
    }

    private static String excerpt(String text) {
        String[] lines = text.strip().split("\n");
        String out;
        if (lines.length > 0 && lines[0].startsWith("- ")) {
            out = String.join(" ", Arrays.stream(lines).limit(3).toList());
        } else {
            out = firstSentences(text.strip().replaceAll("\\s+", " "), 2);
        }
        return out.length() > MAX_EXCERPT ? out.substring(0, MAX_EXCERPT) : out;
    }

    /** The first {@code count} sentences (ending in . ! or ? before a space or the end), or all text if fewer. */
    private static String firstSentences(String flat, int count) {
        int found = 0;
        for (int i = 0; i < flat.length(); i++) {
            char c = flat.charAt(i);
            boolean end = c == '.' || c == '!' || c == '?';
            if (end && (i + 1 == flat.length() || flat.charAt(i + 1) == ' ') && ++found == count) {
                return flat.substring(0, i + 1);
            }
        }
        return flat;
    }
}
