package at.donmatzos.opsdesk.runbook;

import at.donmatzos.opsdesk.ai.search.Runbook;
import at.donmatzos.opsdesk.ai.search.RunbookParser;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Component;

/** Reads the runbooks (Markdown) from the classpath; they are packaged into the jar from docs/runbooks. */
@Component
public class RunbookLoader {

    private final ResourceLoader loader;
    private final String location;

    public RunbookLoader(ResourceLoader loader, @Value("${opsdesk.ai.runbooks.location}") String location) {
        this.loader = loader;
        this.location = location;
    }

    public List<Runbook> load() {
        try {
            Resource[] resources = find();
            List<Runbook> runbooks = Arrays.stream(resources)
                    .filter(Resource::isReadable)
                    .filter(r -> r.getFilename() != null && !isExcluded(r.getFilename()))
                    .map(this::parse)
                    .sorted(Comparator.comparing(Runbook::id))
                    .toList();
            if (runbooks.isEmpty()) {
                throw new IllegalStateException("No runbooks found at " + location);
            }
            return runbooks;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Resource[] find() throws IOException {
        try {
            return ResourcePatternUtils.getResourcePatternResolver(loader).getResources(location);
        } catch (FileNotFoundException e) {
            return new Resource[0];
        }
    }

    private static boolean isExcluded(String name) {
        return !name.endsWith(".md") || name.equals("README.md") || name.endsWith("-template.md");
    }

    private Runbook parse(Resource resource) {
        try {
            String name = resource.getFilename();
            String id = name.substring(0, name.length() - 3);
            return RunbookParser.parse(id, new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
