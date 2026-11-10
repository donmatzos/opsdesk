package at.donmatzos.opsdesk.catalog;

import at.donmatzos.opsdesk.IntegrationTest;
import org.testng.annotations.Test;

public class CatalogApiTest extends IntegrationTest {

    @Test
    public void shouldListDemoServicesSortedByName() {
        client().get().uri("/api/services").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(4)
                .jsonPath("$[0].name").isEqualTo("billing-api")
                .jsonPath("$[0].ownerTeam").isEqualTo("team-payments")
                .jsonPath("$[0].stage").isEqualTo("PROD")
                .jsonPath("$[0].status").isEqualTo("UP");
    }

    @Test
    public void shouldReturn404ProblemWhenServiceIsUnknown() {
        client().get().uri("/api/services/999999").exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType("application/problem+json")
                .expectBody()
                .jsonPath("$.title").isEqualTo("Not Found")
                .jsonPath("$.detail").isEqualTo("Service 999999 not found");
    }
}
