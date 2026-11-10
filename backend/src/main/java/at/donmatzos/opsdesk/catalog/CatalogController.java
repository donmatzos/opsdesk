package at.donmatzos.opsdesk.catalog;

import at.donmatzos.opsdesk.common.NotFoundException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
class CatalogController {

    private final MonitoredServiceRepository services;

    CatalogController(MonitoredServiceRepository services) {
        this.services = services;
    }

    @GetMapping
    List<MonitoredService> list() {
        return services.findAllByOrderByNameAsc();
    }

    @GetMapping("/{id}")
    MonitoredService get(@PathVariable long id) {
        return services.findById(id).orElseThrow(() -> new NotFoundException("Service", id));
    }
}
