package at.donmatzos.opsdesk;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

/** Every observed request runs in a real trace, so the ECS logs carry trace.id / span.id for correlation. */
public class TracingTest extends IntegrationTest {

    @Autowired
    private ObservationRegistry observations;

    @Autowired
    private Tracer tracer;

    @Test
    public void shouldCreateRealTraceIds() {
        Observation.createNotStarted("tracing-test", observations).observe(() -> {
            Span span = tracer.currentSpan();
            assertNotNull(span, "no current span: tracing bridge not active");
            String traceId = span.context().traceId();
            assertTrue(traceId.matches("^[0-9a-f]{32}$"), "not an OpenTelemetry trace id: " + traceId);
        });
    }
}
