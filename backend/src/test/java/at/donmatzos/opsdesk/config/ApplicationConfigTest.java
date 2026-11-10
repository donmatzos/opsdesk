package at.donmatzos.opsdesk.config;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.util.List;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.Test;

/** No password may ever live in application.yml; the default document also has no fallbacks (fail fast). */
public class ApplicationConfigTest {

    @Test
    public void shouldNotDefaultDatasourceCredentials() throws IOException {
        List<PropertySource<?>> documents =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        PropertySource<?> defaults = documents.getFirst();

        assertEquals(String.valueOf(defaults.getProperty("spring.datasource.username")), "${SPRING_DATASOURCE_USERNAME}");
        assertEquals(String.valueOf(defaults.getProperty("spring.datasource.password")), "${SPRING_DATASOURCE_PASSWORD}");
    }

    @Test
    public void shouldNotDefineAPasswordInAnyProfile() throws IOException {
        List<PropertySource<?>> documents =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        for (PropertySource<?> document : documents.subList(1, documents.size())) {
            assertNull(document.getProperty("spring.datasource.password"), document.getName());
        }
    }

    @Test
    public void shouldDisableDatadogByDefault() throws IOException {
        PropertySource<?> defaults = load().getFirst();
        assertEquals(String.valueOf(defaults.getProperty("management.datadog.metrics.export.enabled")), "false");
    }

    @Test
    public void shouldTakeTheDatadogKeyOnlyFromTheEnvironment() throws IOException {
        PropertySource<?> datadog = load().stream()
                .filter(d -> "datadog".equals(d.getProperty("spring.config.activate.on-profile")))
                .findFirst().orElseThrow();
        assertEquals(String.valueOf(datadog.getProperty("management.datadog.metrics.export.api-key")),
                "${DATADOG_API_KEY}");
        assertEquals(String.valueOf(datadog.getProperty("management.datadog.metrics.export.enabled")), "true");
    }

    // The registry posts straight to the Datadog API (no agent enrichment): service/env/version must be tags.
    @Test
    public void shouldTagDatadogMetricsWithUnifiedServiceTags() throws IOException {
        PropertySource<?> datadog = load().stream()
                .filter(d -> "datadog".equals(d.getProperty("spring.config.activate.on-profile")))
                .findFirst().orElseThrow();
        assertEquals(String.valueOf(datadog.getProperty("management.metrics.tags.service")), "${DD_SERVICE:opsdesk}");
        assertEquals(String.valueOf(datadog.getProperty("management.metrics.tags.env")), "${DD_ENV:local}");
        assertEquals(String.valueOf(datadog.getProperty("management.metrics.tags.version")), "${DD_VERSION:unknown}");
    }

    private static List<PropertySource<?>> load() throws IOException {
        return new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
    }
}
