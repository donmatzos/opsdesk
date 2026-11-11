package at.donmatzos.opsdesk.runbook;

import java.util.List;

public record AssistResponse(String answer, List<AssistSource> sources, String model, boolean mock) {
}
