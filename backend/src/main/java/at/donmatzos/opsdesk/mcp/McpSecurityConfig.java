package at.donmatzos.opsdesk.mcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class McpSecurityConfig {

    /** Empty default = MCP disabled (503). Not a credential: the token itself only ever comes from the environment. */
    @Bean
    FilterRegistrationBean<McpTokenFilter> mcpTokenFilter(@Value("${opsdesk.mcp.token:}") String token) {
        var registration = new FilterRegistrationBean<>(new McpTokenFilter(token));
        registration.addUrlPatterns("/mcp", "/mcp/*");
        registration.setName("mcpTokenFilter");
        return registration;
    }
}
