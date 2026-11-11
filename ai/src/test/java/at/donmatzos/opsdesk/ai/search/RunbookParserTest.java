package at.donmatzos.opsdesk.ai.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

public class RunbookParserTest {

    private static final String MD = """
            # Database down

            Intro text.

            ## First checks

            Check the pod.

            ## Mitigation

            Restart it.
            """;

    @Test
    public void shouldTakeTitleFromFirstHeading() {
        assertThat(RunbookParser.parse("db-down", MD).title()).isEqualTo("Database down");
    }

    @Test
    public void shouldSplitChunksAtSecondLevelHeadings() {
        var rb = RunbookParser.parse("db-down", MD);
        assertThat(rb.chunks()).extracting(RunbookChunk::section)
                .containsExactly("Overview", "First checks", "Mitigation");
        assertThat(rb.chunks().get(1).text()).contains("Check the pod.");
        assertThat(rb.chunks().get(2).order()).isEqualTo(2);
    }

    @Test
    public void shouldKeepIntroAsOverview() {
        assertThat(RunbookParser.parse("x", MD).chunks().get(0).text()).contains("Intro text.");
    }

    @Test
    public void shouldNotSplitInsideCodeFences() {
        var md = "# T\n\n## A\n\n```\n## not a heading\n```\n\n## B\n\ntext\n";
        var rb = RunbookParser.parse("x", md);
        assertThat(rb.chunks()).extracting(RunbookChunk::section).containsExactly("A", "B");
        assertThat(rb.chunks().get(0).text()).contains("## not a heading");
    }

    @Test
    public void shouldFallBackToIdWhenTitleIsMissing() {
        assertThat(RunbookParser.parse("no-title", "## A\n\ntext").title()).isEqualTo("no-title");
    }

    @Test
    public void shouldBuildStableChunkIds() {
        var rb = RunbookParser.parse("db-down", MD);
        assertThat(rb.chunks().get(1).id()).isEqualTo("db-down--first-checks");
        assertThat(rb.chunks().get(1).path()).isEqualTo("docs/runbooks/db-down.md");
    }

    @Test
    public void shouldGiveDuplicateSectionsUniqueIds() {
        var rb = RunbookParser.parse("x", "# T\n\nintro\n\n## Overview\n\na\n\n## Verify\n\nb\n\n## Verify\n\nc\n");
        assertThat(rb.chunks()).extracting(RunbookChunk::id).doesNotHaveDuplicates()
                .containsExactly("x--overview", "x--overview-2", "x--verify", "x--verify-2");
    }

    @Test
    public void shouldUseAFallbackSlugForHeadingsWithoutLatinLetters() {
        var rb = RunbookParser.parse("x", "# T\n\n## 概要\n\na\n");
        assertThat(rb.chunks().get(0).id()).isEqualTo("x--section");
    }

    @Test
    public void shouldHandleTildeFencesAndOneLineFences() {
        var md = "# T\n\n## A\n\n~~~\n## not a heading\n~~~\n\n```code```\n\n## B\n\ntext\n";
        var rb = RunbookParser.parse("x", md);
        assertThat(rb.chunks()).extracting(RunbookChunk::section).containsExactly("A", "B");
    }
}
