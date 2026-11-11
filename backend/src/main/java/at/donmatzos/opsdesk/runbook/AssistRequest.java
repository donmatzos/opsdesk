package at.donmatzos.opsdesk.runbook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistRequest(@NotBlank @Size(max = 500) String question) {
}
