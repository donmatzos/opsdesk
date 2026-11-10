package at.donmatzos.opsdesk;

import org.testng.annotations.Test;

public class ApplicationContextTest extends IntegrationTest {

    @Test
    public void shouldReportHealthUp() {
        client().get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    public void shouldExposeLivenessProbe() {
        client().get().uri("/actuator/health/liveness").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    public void shouldExposeReadinessProbe() {
        client().get().uri("/actuator/health/readiness").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }
}
