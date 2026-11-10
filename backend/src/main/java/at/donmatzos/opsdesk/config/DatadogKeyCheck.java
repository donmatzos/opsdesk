package at.donmatzos.opsdesk.config;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * MOCK: no Datadog account exists for this project; the profile shows how metrics would be shipped.
 * With profile "datadog" the API key must come from the environment; without it the start fails at once with a
 * clear message instead of silently dropping metrics.
 */
@Component
@Profile("datadog")
class DatadogKeyCheck {

    DatadogKeyCheck(Environment environment) {
        String key = environment.getProperty("DATADOG_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Profile 'datadog' needs the Datadog API key in the environment variable DATADOG_API_KEY");
        }
    }
}
