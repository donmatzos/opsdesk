package at.donmatzos.opsdesk.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testng.annotations.Test;

/** Review Focus 4 at context level: with profile "datadog" and no key the context does not start. */
public class DatadogKeyCheckWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DatadogKeyCheck.class);

    @Test
    public void shouldStopTheStartWithProfileDatadogAndNoKey() {
        runner.withInitializer(context -> context.getEnvironment().setActiveProfiles("datadog"))
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("DATADOG_API_KEY"));
    }

    @Test
    public void shouldNotCheckWithoutProfileDatadog() {
        runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(DatadogKeyCheck.class));
    }
}
