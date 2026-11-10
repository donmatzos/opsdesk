package at.donmatzos.opsdesk.incident;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import at.donmatzos.opsdesk.IntegrationTest;
import at.donmatzos.opsdesk.catalog.MonitoredServiceRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.testng.annotations.Test;

public class IncidentApiTest extends IntegrationTest {

    private static final String ISO_UTC = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z";

    @Autowired
    private MonitoredServiceRepository services;

    private long billingApiId() {
        return services.findAllByOrderByNameAsc().stream()
                .filter(s -> s.name().equals("billing-api")).findFirst().orElseThrow().id();
    }

    private IncidentResponse open(String summary) {
        IncidentResponse created = client().post().uri("/api/incidents").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("serviceId", billingApiId(), "severity", "SEV2", "summary", summary))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", "http://localhost:\\d+/api/incidents/\\d+")
                .expectBody(IncidentResponse.class).returnResult().getResponseBody();
        assertNotNull(created);
        return created;
    }

    private void patch(long id, String state, int expectedStatus) {
        client().patch().uri("/api/incidents/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("state", state)).exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

    @Test
    public void shouldOpenMitigateAndResolveIncident() {
        long id = open("Checkout returns 502").id();

        patch(id, "MITIGATED", 200);
        client().patch().uri("/api/incidents/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("state", "RESOLVED")).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.resolvedAt").isNotEmpty();
        patch(id, "OPEN", 409);
    }

    @Test
    public void shouldFilterByState() {
        long id = open("Slow invoice export").id();

        assertTrue(idsFor("OPEN").contains(id), "open incident listed under OPEN");
        assertFalse(idsFor("RESOLVED").contains(id), "open incident not listed under RESOLVED");
    }

    private List<Long> idsFor(String state) {
        IncidentResponse[] incidents = client().get().uri("/api/incidents?state={s}", state).exchange()
                .expectStatus().isOk().expectBody(IncidentResponse[].class).returnResult().getResponseBody();
        assertNotNull(incidents);
        return Arrays.stream(incidents).map(IncidentResponse::id).toList();
    }

    @Test
    public void shouldSerializeTimestampsAsIso8601Utc() {
        long id = open("Timestamp format").id();

        client().patch().uri("/api/incidents/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("state", "RESOLVED")).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.openedAt").value(String.class, v -> assertTrue(v.matches(ISO_UTC), v))
                .jsonPath("$.resolvedAt").value(String.class, v -> assertTrue(v.matches(ISO_UTC), v))
                .jsonPath("$.version").doesNotExist();
    }

    @Test
    public void shouldReturn409ProblemWhenStateIsUnchanged() {
        long id = open("Same state").id();

        client().patch().uri("/api/incidents/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("state", "OPEN")).exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().contentType("application/problem+json")
                .expectBody().jsonPath("$.detail").isEqualTo("Cannot change incident state from OPEN to OPEN");
    }

    @Test
    public void shouldReturn422WhenServiceIdIsUnknown() {
        client().post().uri("/api/incidents").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("serviceId", 999999, "severity", "SEV2", "summary", "Ghost"))
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody().jsonPath("$.detail").isEqualTo("Service 999999 does not exist");
    }

    @Test
    public void shouldReturn404WhenIncidentIsUnknown() {
        patch(999999, "MITIGATED", 404);
    }
}
