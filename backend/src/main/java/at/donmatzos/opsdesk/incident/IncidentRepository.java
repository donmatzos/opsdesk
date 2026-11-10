package at.donmatzos.opsdesk.incident;

import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

public interface IncidentRepository extends ListCrudRepository<Incident, Long> {

    List<Incident> findAllByOrderByOpenedAtDesc();

    List<Incident> findByStateOrderByOpenedAtDesc(IncidentState state);
}
