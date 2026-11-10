package at.donmatzos.opsdesk.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

public interface MonitoredServiceRepository extends ListCrudRepository<MonitoredService, Long> {

    List<MonitoredService> findAllByOrderByNameAsc();

    List<MonitoredService> findByStageOrderByNameAsc(Stage stage);

    Optional<MonitoredService> findByName(String name);
}
