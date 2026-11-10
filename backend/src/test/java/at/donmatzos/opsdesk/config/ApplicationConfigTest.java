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
}
