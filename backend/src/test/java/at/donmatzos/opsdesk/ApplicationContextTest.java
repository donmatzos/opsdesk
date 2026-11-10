package at.donmatzos.opsdesk;

import org.testng.annotations.Test;

public class ApplicationContextTest extends IntegrationTest {

    @Test
    public void shouldReportHealthUp() {
        client().get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }
}
