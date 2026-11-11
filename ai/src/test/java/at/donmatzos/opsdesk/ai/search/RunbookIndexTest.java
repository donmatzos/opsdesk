package at.donmatzos.opsdesk.ai.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.testng.annotations.Test;

public class RunbookIndexTest {

    private static Runbook rb(String id, String md) {
        return RunbookParser.parse(id, md);
    }

    private final RunbookIndex index = new RunbookIndex(List.of(
            rb("database-unavailable", "# Database unavailable\n\n## Symptoms\n\nPostgres connection refused, timeouts.\n"),
            rb("pod-not-ready", "# Pod not ready\n\n## Symptoms\n\nReadiness probe failing after deploy.\n"),
            rb("http-errors", "# HTTP errors\n\n## Symptoms\n\nA rising share of 5xx responses.\n")));

    @Test
    public void shouldRankTheMatchingSectionFirst() {
        var hits = index.search("postgres connection refused", 3);
        assertThat(hits.get(0).chunk().runbookId()).isEqualTo("database-unavailable");
    }

    @Test
    public void shouldBoostHeadingMatches() {
        var idx = new RunbookIndex(List.of(
                rb("a", "# A\n\n## Other\n\nrollback alpha beta gamma\n"),
                rb("b", "# B\n\n## Rollback\n\nalpha beta gamma delta\n")));
        assertThat(idx.search("rollback", 2).get(0).chunk().runbookId()).isEqualTo("b");
    }

    @Test
    public void shouldReturnNothingWithoutMatch() {
        assertThat(index.search("xyzzy quux", 3)).isEmpty();
        assertThat(index.search("  ", 3)).isEmpty();
    }

    @Test
    public void shouldRespectLimit() {
        assertThat(index.search("symptoms", 2)).hasSize(2);
    }

    @Test
    public void shouldOrderTiesDeterministically() {
        var idx = new RunbookIndex(List.of(
                rb("b", "# T\n\n## S\n\nsame words\n"), rb("a", "# T\n\n## S\n\nsame words\n")));
        assertThat(idx.search("same words", 5)).extracting(h -> h.chunk().runbookId()).containsExactly("a", "b");
        assertThat(idx.size()).isEqualTo(2);
    }
}
