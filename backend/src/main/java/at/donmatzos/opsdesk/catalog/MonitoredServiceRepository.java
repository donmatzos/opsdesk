package at.donmatzos.opsdesk.catalog;

import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

public interface MonitoredServiceRepository extends ListCrudRepository<MonitoredService, Long> {

    List<MonitoredService> findAllByOrderByNameAsc();
}
