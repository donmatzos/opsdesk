package at.donmatzos.opsdesk.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testng.annotations.Test;

/** Web layer only: request validation and error mapping, with the service mocked. */
@WebMvcTest(IncidentController.class)
public class IncidentControllerTest extends AbstractTestNGSpringContextTests {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private IncidentService service;

    private MvcTestResult post(String json) {
        return mvc.post().uri("/api/incidents").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    @Test
    public void shouldReturn400ForUnknownSeverity() {
        assertThat(post("{\"serviceId\":1,\"severity\":\"SEV9\",\"summary\":\"x\"}"))
                .hasStatus(400).hasContentType(PROBLEM_JSON);
    }

    @Test
    public void shouldReturn400ForBlankSummary() {
        assertThat(post("{\"serviceId\":1,\"severity\":\"SEV2\",\"summary\":\" \"}"))
                .hasStatus(400).hasContentType(PROBLEM_JSON)
                .bodyJson().extractingPath("$.errors.summary").isNotNull();
    }

    @Test
    public void shouldReturn400ForSummaryOver200Chars() {
        assertThat(post("{\"serviceId\":1,\"severity\":\"SEV2\",\"summary\":\"" + "x".repeat(201) + "\"}"))
                .hasStatus(400).hasContentType(PROBLEM_JSON)
                .bodyJson().extractingPath("$.errors.summary").isNotNull();
    }

    @Test
    public void shouldReturn400ForUnknownStateFilter() {
        assertThat(mvc.get().uri("/api/incidents?state=foo").exchange())
                .hasStatus(400).hasContentType(PROBLEM_JSON);
    }

    @Test
    public void shouldReturn409WhenServiceRejectsTransition() {
        when(service.changeState(eq(7L), eq(IncidentState.OPEN)))
                .thenThrow(new IllegalStateTransitionException(IncidentState.RESOLVED, IncidentState.OPEN));

        assertThat(mvc.patch().uri("/api/incidents/7").contentType(MediaType.APPLICATION_JSON)
                .content("{\"state\":\"OPEN\"}").exchange())
                .hasStatus(409).hasContentType(PROBLEM_JSON)
                .bodyJson().extractingPath("$.detail").isEqualTo("Cannot change incident state from RESOLVED to OPEN");
    }

    @Test
    public void shouldReturn409WhenIncidentWasChangedConcurrently() {
        when(service.changeState(eq(7L), eq(IncidentState.RESOLVED)))
                .thenThrow(new OptimisticLockingFailureException("stale"));

        assertThat(mvc.patch().uri("/api/incidents/7").contentType(MediaType.APPLICATION_JSON)
                .content("{\"state\":\"RESOLVED\"}").exchange())
                .hasStatus(409).hasContentType(PROBLEM_JSON)
                .bodyJson().extractingPath("$.detail")
                .isEqualTo("The incident was changed by someone else; reload and try again");
    }

    @Test
    public void shouldReturnGeneric500ProblemWithoutInternalDetails() {
        when(service.list(any())).thenThrow(new IllegalStateException("connection to db-secret-host failed"));

        MvcTestResult result = mvc.get().uri("/api/incidents").exchange();
        assertThat(result).hasStatus(500).hasContentType(PROBLEM_JSON);
        assertThat(result).bodyText().doesNotContain("db-secret-host");
    }

    @Test
    public void shouldReturn422ForUnknownService() {
        when(service.open(any())).thenThrow(new UnknownServiceException(42L));

        assertThat(post("{\"serviceId\":42,\"severity\":\"SEV2\",\"summary\":\"x\"}"))
                .hasStatus(422).hasContentType(PROBLEM_JSON)
                .bodyJson().extractingPath("$.detail").isEqualTo("Service 42 does not exist");
    }
}
