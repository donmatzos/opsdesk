package at.donmatzos.opsdesk.runbook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import at.donmatzos.opsdesk.ai.AiBackendException;
import at.donmatzos.opsdesk.ai.RunbookAssistant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testng.annotations.Test;

/** Web layer only: validation and error mapping, with the assistant mocked. */
@WebMvcTest(AssistController.class)
public class AssistControllerTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RunbookAssistant assistant;

    private MvcTestResult post(String json) {
        return mvc.post().uri("/api/assist").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    @Test
    public void shouldReturn400WhenQuestionIsBlank() {
        assertThat(post("{\"question\":\"  \"}")).hasStatus(400);
    }

    @Test
    public void shouldReturn400WhenQuestionExceeds500Chars() {
        assertThat(post("{\"question\":\"" + "x".repeat(501) + "\"}")).hasStatus(400);
    }

    @Test
    public void shouldReturn503WithoutInternalsWhenBackendFails() throws Exception {
        when(assistant.ask(anyString())).thenThrow(new AiBackendException("secret-host failed"));
        var result = post("{\"question\":\"pod not ready\"}");
        assertThat(result).hasStatus(503);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("secret-host")
                .contains("The assistant backend is unavailable; try again later");
    }
}
