package at.donmatzos.opsdesk.ai.search;

import static org.assertj.core.api.Assertions.assertThat;

import at.donmatzos.opsdesk.ai.AzureSettings;
import at.donmatzos.opsdesk.ai.FakeServer;
import java.net.http.HttpClient;
import java.util.List;
import org.testng.annotations.Test;

public class AzureAiSearchIndexerTest {

    @Test
    public void shouldUploadChunksWithMergeOrUpload() throws Exception {
        try (var server = new FakeServer(200, "{\"value\":[]}")) {
            var s = new AzureSettings(server.uri(), "d", server.uri(), "my-index");
            new AzureAiSearchIndexer(s, FakeServer.credentials("k"), HttpClient.newHttpClient())
                    .upload(List.of(RunbookParser.parse("a", "# A\n\n## S\n\ntext\n")));

            var seen = server.seen.get();
            assertThat(seen.path()).isEqualTo("/indexes/my-index/docs/index?api-version=2024-07-01");
            assertThat(seen.body()).contains("\"@search.action\":\"mergeOrUpload\"").contains("\"id\":\"a--s\"")
                    .contains("\"content\":\"text\"").contains("\"runbook\":\"a\"");
        }
    }
}
