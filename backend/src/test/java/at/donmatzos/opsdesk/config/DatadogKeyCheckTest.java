package at.donmatzos.opsdesk.config;

import org.springframework.mock.env.MockEnvironment;
import org.testng.annotations.Test;

/** Review Focus 4: profile "datadog" without a key must stop the start, not silently drop metrics. */
public class DatadogKeyCheckTest {

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*DATADOG_API_KEY.*")
    public void shouldFailFastWithoutApiKey() {
        new DatadogKeyCheck(new MockEnvironment());
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*DATADOG_API_KEY.*")
    public void shouldFailFastWithBlankApiKey() {
        new DatadogKeyCheck(new MockEnvironment().withProperty("DATADOG_API_KEY", "  "));
    }

    @Test
    public void shouldAcceptAKeyFromTheEnvironment() {
        new DatadogKeyCheck(new MockEnvironment().withProperty("DATADOG_API_KEY", "from-env"));
    }
}
