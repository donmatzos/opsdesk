package at.donmatzos.opsdesk.ai.llm;

import at.donmatzos.opsdesk.ai.search.RetrievedChunk;
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
            out = String.join(" ", java.util.Arrays.stream(lines).limit(3).toList());
        } else {
            String flat = text.strip().replaceAll("\\s+", " ");
            var m = java.util.regex.Pattern.compile("^(.*?[.!?])(\\s+.*?[.!?])?(\\s|$)").matcher(flat);
            out = m.find() ? (m.group(1) + (m.group(2) == null ? "" : m.group(2))) : flat;
        }
        return out.length() > MAX_EXCERPT ? out.substring(0, MAX_EXCERPT) : out;
    }
}
