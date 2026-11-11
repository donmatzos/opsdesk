package at.donmatzos.opsdesk.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.testng.annotations.Test;

public class AzureSettingsTest {

    static Map<String, String> full() {
        Map<String, String> env = new HashMap<>();
        env.put("AZURE_OPENAI_ENDPOINT", "https://oai.example.test");
        env.put("AZURE_OPENAI_API_KEY", UUID.randomUUID().toString());
        env.put("AZURE_OPENAI_DEPLOYMENT", "chat");
        env.put("AZURE_SEARCH_ENDPOINT", "https://search.example.test/");
        env.put("AZURE_SEARCH_API_KEY", UUID.randomUUID().toString());
        env.put("AZURE_SEARCH_INDEX", "idx");
        return env;
    }

    @Test
    public void shouldListAllMissingVariablesAtOnce() {
        assertThatThrownBy(() -> AzureSettings.from(k -> null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AZURE_OPENAI_ENDPOINT").hasMessageContaining("AZURE_OPENAI_API_KEY")
                .hasMessageContaining("AZURE_OPENAI_DEPLOYMENT").hasMessageContaining("AZURE_SEARCH_ENDPOINT")
                .hasMessageContaining("AZURE_SEARCH_API_KEY").hasMessageContaining("AZURE_SEARCH_INDEX");
    }

    @Test
    public void shouldTreatBlankAsMissing() {
        var env = full();
        env.put("AZURE_SEARCH_INDEX", "  ");
        assertThatThrownBy(() -> AzureSettings.from(env::get)).hasMessageContaining("AZURE_SEARCH_INDEX")
                .hasMessageNotContaining("AZURE_OPENAI");
    }

    @Test
    public void shouldRejectPlainHttpEndpoints() {
        var env = full();
        env.put("AZURE_OPENAI_ENDPOINT", "http://oai.example.test");
        assertThatThrownBy(() -> AzureSettings.from(env::get)).hasMessageContaining("https");
        env.put("AZURE_OPENAI_ENDPOINT", "http://localhost:1");
        assertThat(AzureSettings.from(env::get).openAiEndpoint().getPort()).isEqualTo(1);
    }

    @Test
    public void shouldNotHoldApiKeysInSettings() {
        var env = full();
        var settings = AzureSettings.from(env::get);
        assertThat(settings.toString()).doesNotContain(env.get("AZURE_OPENAI_API_KEY"))
                .doesNotContain(env.get("AZURE_SEARCH_API_KEY"));
        assertThat(AzureSettings.class.getRecordComponents()).extracting(c -> c.getName().toLowerCase())
                .noneMatch(n -> n.contains("key") || n.contains("secret"));
    }

    @Test
    public void shouldReadKeysFromTheEnvironmentOnEveryCallAndNeverPrintThem() {
        var env = full();
        var credentials = AzureCredentials.fromEnvironment(env::get);
        assertThat(credentials.openAiApiKey()).isEqualTo(env.get("AZURE_OPENAI_API_KEY"));
        String rotated = UUID.randomUUID().toString();
        env.put("AZURE_SEARCH_API_KEY", rotated);
        assertThat(credentials.searchApiKey()).isEqualTo(rotated);
        assertThat(credentials.toString()).doesNotContain(rotated).doesNotContain(env.get("AZURE_OPENAI_API_KEY"));
        env.put("AZURE_SEARCH_API_KEY", " ");
        assertThatThrownBy(credentials::searchApiKey).isInstanceOf(AiBackendException.class)
                .hasMessage("AZURE_SEARCH_API_KEY is not set");
    }

    @Test
    public void shouldStripTrailingSlashFromEndpoints() {
        assertThat(AzureSettings.from(full()::get).searchEndpoint().toString())
                .isEqualTo("https://search.example.test");
    }
}
