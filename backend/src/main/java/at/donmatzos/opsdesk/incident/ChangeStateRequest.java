package at.donmatzos.opsdesk.incident;

import jakarta.validation.constraints.NotNull;

public record ChangeStateRequest(@NotNull IncidentState state) {
}
