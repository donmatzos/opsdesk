package at.donmatzos.opsdesk.mcp;

import jakarta.servlet.FilterChain;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class McpTokenFilterTest {

    private static final String TOKEN = java.util.UUID.randomUUID().toString();

    private static MockHttpServletRequest request(String authorization) {
        var request = new MockHttpServletRequest("POST", "/mcp");
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }

    private static MockHttpServletResponse run(McpTokenFilter filter, MockHttpServletRequest request,
                                               MockFilterChain chain) throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    public void shouldPassThroughWhenTokenMatches() throws Exception {
        var chain = new MockFilterChain();
        var response = run(new McpTokenFilter(TOKEN), request("Bearer " + TOKEN), chain);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    public void shouldRejectWithoutHeader() throws Exception {
        var chain = new MockFilterChain();
        var response = run(new McpTokenFilter(TOKEN), request(null), chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    public void shouldRejectWrongToken() throws Exception {
        var chain = new MockFilterChain();
        var response = run(new McpTokenFilter(TOKEN), request("Bearer " + TOKEN + "x"), chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
        assertThat(response.getContentAsString()).doesNotContain(TOKEN);
    }

    @Test
    public void shouldRejectOtherSchemeAndEmptyBearer() throws Exception {
        for (String header : new String[] {"Basic " + TOKEN, "Bearer ", "Bearer", TOKEN}) {
            var chain = new MockFilterChain();
            var response = run(new McpTokenFilter(TOKEN), request(header), chain);
            assertThat(response.getStatus()).as(header).isEqualTo(401);
            assertThat(chain.getRequest()).isNull();
        }
    }

    @Test
    public void shouldAnswer503WhenTokenUnset() throws Exception {
        for (String token : new String[] {"", "  ", null}) {
            var chain = new MockFilterChain();
            var response = run(new McpTokenFilter(token), request("Bearer anything"), chain);
            assertThat(response.getStatus()).isEqualTo(503);
            assertThat(response.getContentType()).startsWith("application/problem+json");
            assertThat(response.getContentAsString()).contains("MCP is disabled: set OPSDESK_MCP_TOKEN");
            assertThat(chain.getRequest()).isNull();
        }
    }

    @Test
    public void shouldNotInvokeChainOnRejection() throws Exception {
        FilterChain chain = (req, res) -> {
            throw new AssertionError("chain must not run");
        };
        new McpTokenFilter(TOKEN).doFilter(request("Bearer nope"), new MockHttpServletResponse(), chain);
    }

    @Test
    public void shouldIgnoreWhitespaceAroundConfiguredToken() throws Exception {
        var chain = new MockFilterChain();
        var response = run(new McpTokenFilter(TOKEN + "\n"), request("Bearer " + TOKEN), chain);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    public void shouldTreatWhitespaceOnlyTokenAsDisabled() throws Exception {
        var chain = new MockFilterChain();
        var response = run(new McpTokenFilter(" \n\t "), request("Bearer anything"), chain);
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(chain.getRequest()).isNull();
    }
}
