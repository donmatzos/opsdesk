package at.donmatzos.opsdesk.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import at.donmatzos.opsdesk.ai.search.RetrievedChunk;
import at.donmatzos.opsdesk.ai.search.Retriever;
import at.donmatzos.opsdesk.ai.search.RunbookChunk;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Plain unit test: the retriever is mocked, no Spring context. */
public class RunbookToolsTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private Retriever retriever;
    private RunbookTools tools;

    @BeforeMethod
    public void setUp() {
        retriever = mock(Retriever.class);
        tools = new RunbookTools(retriever);
    }

    private static String text(CallToolResult r) {
        return ((TextContent) r.content().getFirst()).text();
    }

    private static RetrievedChunk hit(String text) {
        return new RetrievedChunk(new RunbookChunk("pod-not-ready--symptoms", "pod-not-ready", "Pod not ready",
                "Symptoms", text, 1), 1.5);
    }

    @Test
    public void shouldMapHitsAndCutExcerpts() {
        String longText = ("word ".repeat(200)).strip();
        when(retriever.search("pod not ready", 3)).thenReturn(List.of(hit(longText), hit("short")));

        CallToolResult result = tools.searchRunbooks("pod not ready", null);

        assertThat(result.isError()).isFalse();
        JsonNode hits = JSON.readTree(text(result)).get("hits");
        assertThat(hits).hasSize(2);
        assertThat(hits.get(0).get("runbook").asString()).isEqualTo("pod-not-ready");
        assertThat(hits.get(0).get("path").asString()).isEqualTo("docs/runbooks/pod-not-ready.md");
        String excerpt = hits.get(0).get("excerpt").asString();
        assertThat(excerpt.length()).isLessThanOrEqualTo(401);
        assertThat(excerpt).endsWith("…").doesNotContain("wor…");
        assertThat(hits.get(1).get("excerpt").asString()).isEqualTo("short");
    }

    @Test
    public void shouldRejectBlankQuery() {
        CallToolResult result = tools.searchRunbooks("  ", 3);
        assertThat(result.isError()).isTrue();
        verifyNoInteractions(retriever);
    }

    @Test
    public void shouldRejectLimitOutsideOneToFive() {
        assertThat(tools.searchRunbooks("x", 0).isError()).isTrue();
        assertThat(tools.searchRunbooks("x", 6).isError()).isTrue();
        verifyNoInteractions(retriever);
    }

    @Test
    public void shouldDefaultLimitToThree() {
        when(retriever.search(anyString(), anyInt())).thenReturn(List.of());
        assertThat(tools.searchRunbooks("x", null).isError()).isFalse();
        verify(retriever).search("x", 3);
    }
}
