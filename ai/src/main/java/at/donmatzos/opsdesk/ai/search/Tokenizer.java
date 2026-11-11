package at.donmatzos.opsdesk.ai.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Lowercases, splits on non-alphanumerics, drops stop words and folds simple plurals. */
public final class Tokenizer {

    private static final Set<String> STOP_WORDS = Set.of("a", "an", "and", "are", "as", "at", "be", "by", "for",
            "from", "how", "i", "in", "is", "it", "of", "on", "or", "the", "to", "what", "when", "where", "which",
            "why", "with");

    private Tokenizer() {
    }

    public static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        for (String t : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (t.length() < 2 || STOP_WORDS.contains(t)) {
                continue;
            }
            out.add(t.length() > 3 && t.endsWith("s") && !t.endsWith("ss") ? t.substring(0, t.length() - 1) : t);
        }
        return out;
    }
}
