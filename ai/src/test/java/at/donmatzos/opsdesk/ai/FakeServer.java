package at.donmatzos.opsdesk.ai;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/** Local stand-in for an Azure endpoint: records the last request and replies with a canned answer. */
public final class FakeServer implements AutoCloseable {

    public record Seen(String method, String path, String apiKey, String body) {
    }

    public final AtomicReference<Seen> seen = new AtomicReference<>();
    private final HttpServer server;

    public FakeServer(int status, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            seen.set(new Seen(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                    exchange.getRequestHeaders().getFirst("api-key"), body));
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    /** Credentials for tests: the key comes from a fake environment, like in the app. */
    public static AzureCredentials credentials(String key) {
        return AzureCredentials.fromEnvironment(name -> key);
    }

    public URI uri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
