package at.donmatzos.opsdesk;

import org.testng.annotations.Test;

public class ApplicationContextTest extends IntegrationTest {

    @Test
    public void shouldReportHealthUp() {
        management().get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    public void shouldExposeLivenessProbe() {
        management().get().uri("/actuator/health/liveness").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    public void shouldExposeReadinessProbe() {
        management().get().uri("/actuator/health/readiness").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    // Probes stay on the app port (/livez, /readyz), so Kubernetes and the smoke test need no management port.
    @Test
    public void shouldServeProbesOnTheAppPort() {
        client().get().uri("/livez").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
        client().get().uri("/readyz").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    public void shouldNotServeActuatorOnTheAppPort() {
        client().get().uri("/actuator/prometheus").exchange().expectStatus().isNotFound();
        client().get().uri("/actuator/health").exchange().expectStatus().isNotFound();
    }
}
