package at.donmatzos.opsdesk.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(@NotNull Long serviceId, @NotNull Severity severity,
                                    @NotBlank @Size(max = 200) String summary) {
}
