package at.donmatzos.opsdesk.mcp;

import at.donmatzos.opsdesk.ai.search.Retriever;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/** MCP tool over the runbooks (read-only). Uses the same retriever bean as {@code POST /api/assist}. */
@Component
class RunbookTools {

    static final int DEFAULT_LIMIT = 3;
    static final int MAX_LIMIT = 5;
    static final int MAX_EXCERPT = 400;

    record RunbookHit(String runbook, String title, String section, String excerpt, double score, String path) {
    }

    record RunbookSearchResult(List<RunbookHit> hits) {
    }

    private final Retriever retriever;

    RunbookTools(Retriever retriever) {
        this.retriever = retriever;
    }

    @McpTool(name = "search_runbooks",
            description = """
                    Search the opsdesk operations runbooks. Call this before suggesting how to mitigate an \
                    incident or when the user asks how to handle an alert, outage or error. Returns the \
                    best-matching runbook sections with an excerpt and the repo path of the runbook. Read-only.""",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    CallToolResult searchRunbooks(
            @McpToolParam(description = "Keywords or a symptom, for example: pod not ready after deploy.")
            String query,
            @McpToolParam(required = false, description = "Maximum number of sections, 1 to 5. Default 3.")
            Integer limit) {
        return McpResults.run(() -> {
            if (query == null || query.isBlank()) {
                return McpResults.error("query must not be blank. Give keywords or a symptom.");
            }
            int max = limit == null ? DEFAULT_LIMIT : limit;
            if (max < 1 || max > MAX_LIMIT) {
                return McpResults.error("limit must be between 1 and " + MAX_LIMIT + ", got " + max + ".");
            }
            var hits = retriever.search(query, max).stream()
                    .map(h -> new RunbookHit(h.chunk().runbookId(), h.chunk().title(), h.chunk().section(),
                            excerpt(h.chunk().text()), h.score(), h.chunk().path()))
                    .toList();
            return McpResults.ok(new RunbookSearchResult(hits));
        });
    }

    static String excerpt(String text) {
        String flat = text.strip();
        if (flat.length() <= MAX_EXCERPT) {
            return flat;
        }
        int cut = flat.lastIndexOf(' ', MAX_EXCERPT);
        return flat.substring(0, cut > 0 ? cut : MAX_EXCERPT) + "…";
    }
}
