package at.donmatzos.opsdesk.runbook;

import at.donmatzos.opsdesk.IntegrationTest;
import org.springframework.http.MediaType;
import org.testng.annotations.Test;

public class AssistApiTest extends IntegrationTest {

    private org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec ask(String q) {
        return client().post().uri("/api/assist").contentType(MediaType.APPLICATION_JSON)
                .body("{\"question\":\"" + q + "\"}").exchange();
    }

    @Test
    public void shouldAnswerWithSourcesFromTheShippedRunbooks() {
        ask("database connection refused").expectStatus().isOk().expectBody()
                .jsonPath("$.sources[0].runbook").isEqualTo("database-unavailable")
                .jsonPath("$.sources[0].path").isEqualTo("docs/runbooks/database-unavailable.md")
                .jsonPath("$.mock").isEqualTo(true)
                .jsonPath("$.model").isEqualTo("mock")
                .jsonPath("$.answer").value(a -> org.assertj.core.api.Assertions.assertThat((String) a).contains("[1]"));
    }

    @Test
    public void shouldAnswerWithoutSourcesWhenNothingMatches() {
        ask("xyzzy quux").expectStatus().isOk().expectBody()
                .jsonPath("$.sources.length()").isEqualTo(0)
                .jsonPath("$.answer").isEqualTo("No runbook covers this question yet.");
    }
}
