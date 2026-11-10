package at.donmatzos.opsdesk.catalog;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** A service the desk watches. Named MonitoredService to avoid confusion with Spring's {@code @Service}. */
@Table("services")
public record MonitoredService(@Id Long id, String name, String ownerTeam, Stage stage, ServiceStatus status) {
}
