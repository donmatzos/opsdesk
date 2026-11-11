package at.donmatzos.opsdesk.ai.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** In-memory BM25 index over runbook chunks. Headings count double. */
public final class RunbookIndex implements Retriever {

    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private final List<RunbookChunk> chunks = new ArrayList<>();
    private final List<Map<String, Integer>> termFrequencies = new ArrayList<>();
    private final List<Integer> lengths = new ArrayList<>();
    private final Map<String, Integer> documentFrequency = new HashMap<>();
    private final double averageLength;

    public RunbookIndex(List<Runbook> runbooks) {
        Objects.requireNonNull(runbooks);
        long total = 0;
        for (Runbook runbook : runbooks) {
            for (RunbookChunk chunk : runbook.chunks()) {
                List<String> terms = new ArrayList<>(Tokenizer.tokens(chunk.text()));
                for (int i = 0; i < 2; i++) {
                    terms.addAll(Tokenizer.tokens(chunk.title()));
                    terms.addAll(Tokenizer.tokens(chunk.section()));
                }
                Map<String, Integer> tf = new HashMap<>();
                terms.forEach(t -> tf.merge(t, 1, Integer::sum));
                tf.keySet().forEach(t -> documentFrequency.merge(t, 1, Integer::sum));
                chunks.add(chunk);
                termFrequencies.add(tf);
                lengths.add(terms.size());
                total += terms.size();
            }
        }
        this.averageLength = chunks.isEmpty() ? 1 : Math.max(1, (double) total / chunks.size());
    }

    public int size() {
        return chunks.size();
    }

    @Override
    public List<RetrievedChunk> search(String query, int limit) {
        List<String> queryTerms = Tokenizer.tokens(query == null ? "" : query);
        if (queryTerms.isEmpty()) {
            return List.of();
        }
        int max = Math.clamp(limit, 1, 10);
        int n = chunks.size();
        List<RetrievedChunk> hits = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double score = 0;
            Map<String, Integer> tf = termFrequencies.get(i);
            for (String term : queryTerms) {
                Integer f = tf.get(term);
                if (f == null) {
                    continue;
                }
                int df = documentFrequency.get(term);
                double idf = Math.log(1 + (n - df + 0.5) / (df + 0.5));
                double norm = f + K1 * (1 - B + B * lengths.get(i) / averageLength);
                score += idf * f * (K1 + 1) / norm;
            }
            if (score > 0) {
                hits.add(new RetrievedChunk(chunks.get(i), score));
            }
        }
        hits.sort(Comparator.comparingDouble(RetrievedChunk::score).reversed()
                .thenComparing(h -> h.chunk().runbookId())
                .thenComparingInt(h -> h.chunk().order()));
        return List.copyOf(hits.subList(0, Math.min(max, hits.size())));
    }
}
