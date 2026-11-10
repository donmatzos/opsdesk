package at.donmatzos.opsdesk.mcp;

import at.donmatzos.opsdesk.common.ConflictException;
import at.donmatzos.opsdesk.common.NotFoundException;
import at.donmatzos.opsdesk.common.UnprocessableException;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shared result handling of the MCP tools: compact JSON text results, capped lists, and domain errors returned as
 * tool errors ({@code isError: true}) so the model can read the message and correct itself. Nothing else is
 * exposed: unexpected failures are logged and reported generically (no stack traces, no SQL).
 */
final class McpResults {

    /** Lists never return more entries than this; {@code truncated} tells the model there are more. */
    static final int MAX_LIST_SIZE = 50;

    private static final Logger log = LoggerFactory.getLogger(McpResults.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private McpResults() {
    }

    /** A list result capped at {@link #MAX_LIST_SIZE}. */
    record Capped<T>(List<T> items, boolean truncated) {
    }

    static <T> Capped<T> cap(List<T> all) {
        return new Capped<>(all.stream().limit(MAX_LIST_SIZE).toList(), all.size() > MAX_LIST_SIZE);
    }

    static CallToolResult ok(Object value) {
        return CallToolResult.builder().addTextContent(JSON.writeValueAsString(value)).build();
    }

    static CallToolResult error(String message) {
        return CallToolResult.builder().isError(true).addTextContent(message).build();
    }

    /** Runs a tool body; known domain exceptions become tool errors carrying their message. */
    static CallToolResult run(Supplier<CallToolResult> body) {
        try {
            return body.get();
        } catch (NotFoundException | ConflictException | UnprocessableException e) {
            return error(e.getMessage());
        } catch (RuntimeException e) {
            log.atError().setMessage("MCP tool failed").setCause(e).log();
            return error("The operation failed unexpectedly. Try again later.");
        }
    }
}
