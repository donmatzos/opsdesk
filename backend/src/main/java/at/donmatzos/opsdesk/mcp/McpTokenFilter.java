package at.donmatzos.opsdesk.mcp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protects the MCP endpoint with one shared bearer token (opsdesk.mcp.token, env OPSDESK_MCP_TOKEN).
 * No token configured: MCP is disabled (503). A missing or wrong token gets 401 and nothing else runs: the
 * filter chain is not invoked. The token is never logged or echoed. Registered for /mcp and /mcp/* only
 * (see McpSecurityConfig), so the REST API and the UI are not affected.
 */
public class McpTokenFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final byte[] expected;

    public McpTokenFilter(String token) {
        this.expected = token == null || token.isBlank() ? null : token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (expected == null) {
            problem(response, 503, "Service Unavailable", "MCP is disabled: set OPSDESK_MCP_TOKEN");
            return;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIX)
                || !MessageDigest.isEqual(expected, header.substring(PREFIX.length()).getBytes(StandardCharsets.UTF_8))) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            problem(response, 401, "Unauthorized", "Missing or invalid bearer token");
            return;
        }
        chain.doFilter(request, response);
    }

    private static void problem(HttpServletResponse response, int status, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
                + ",\"detail\":\"" + detail + "\"}");
    }
}
