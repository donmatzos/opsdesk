package at.donmatzos.opsdesk.ai.search;

import static org.assertj.core.api.Assertions.assertThat;

import at.donmatzos.opsdesk.ai.AzureSettings;
import at.donmatzos.opsdesk.ai.FakeServer;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.testng.annotations.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The index definition in infra/terraform/azure must contain every field the retriever selects and the indexer writes. */
public class SearchIndexContractTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    public void shouldMatchTheIndexDefinition() throws Exception {
        JsonNode index = JSON.readTree(Files.readString(Path.of("../infra/terraform/azure/search-index.json")));
        Set<String> fields = new java.util.HashSet<>();
        index.get("fields").forEach(f -> fields.add(f.get("name").asString()));
        assertThat(index.get("name").asString()).isEqualTo("opsdesk-runbooks");

        try (var server = new FakeServer(200, "{\"value\":[]}")) {
            var s = new AzureSettings(server.uri(), "k", "d", server.uri(), "k", "i");
            var http = HttpClient.newHttpClient();
            new AzureAiSearchRetriever(s, http).search("x", 1);
            List<String> selected = List.of(JSON.readTree(server.seen.get().body()).get("select").asString().split(","));
            assertThat(fields).containsAll(selected);

            new AzureAiSearchIndexer(s, http).upload(List.of(RunbookParser.parse("a", "# A\n\n## S\n\ntext\n")));
            List<String> written = new ArrayList<>();
            JSON.readTree(server.seen.get().body()).get("value").get(0).propertyNames().forEach(written::add);
            written.remove("@search.action");
            assertThat(fields).containsExactlyInAnyOrderElementsOf(written);
        }
    }
}
