package at.donmatzos.opsdesk;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/** Base class for full-stack tests: real server on a random port, PostgreSQL in a container, demo data. */
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = "management.server.port=0")
@Import(TestcontainersConfig.class)
@ActiveProfiles("demo")
public abstract class IntegrationTest extends AbstractTestNGSpringContextTests {

    @LocalServerPort
    private int port;

    @Value("${local.management.port}")
    private int managementPort;

    protected RestTestClient client() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    /** Actuator runs on its own port (not routed to the outside), like in the cluster. */
    protected RestTestClient management() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + managementPort).build();
    }
}
