package at.donmatzos.opsdesk.config;

import static org.testng.Assert.assertTrue;

import at.donmatzos.opsdesk.IntegrationTest;
import org.springframework.http.MediaType;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** A browser refresh on an Angular route must get index.html; unknown API paths stay JSON 404s. */
public class SpaRoutingTest extends IntegrationTest {

    @DataProvider
    public static Object[][] angularRoutes() {
        return new Object[][] {{"/services"}, {"/incidents"}, {"/incidents/7"}, {"/assist"}, {"/guide"},
                {"/guide/architecture"}, {"/guide/skills"}, {"/guide/design"}, {"/guide/glossary"}};
    }

    @Test(dataProvider = "angularRoutes")
    public void shouldServeIndexHtmlWhenDeepLinkIsRefreshed(String route) {
        client().get().uri(route).accept(MediaType.TEXT_HTML).exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class).value(body -> assertTrue(body.contains("<app-root")));
    }

    @Test
    public void shouldKeepProblemDetailWhenApiPathIsUnknown() {
        client().get().uri("/api/unknown").exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType("application/problem+json");
    }
}
