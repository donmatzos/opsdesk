package at.donmatzos.opsdesk.incident;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
class IncidentController {

    private final IncidentService service;

    IncidentController(IncidentService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<Incident> open(@Valid @RequestBody CreateIncidentRequest request) {
        Incident created = service.open(request);
        return ResponseEntity.created(URI.create("/api/incidents/" + created.id())).body(created);
    }

    @GetMapping
    List<Incident> list(@RequestParam(required = false) @Nullable IncidentState state) {
        return service.list(state);
    }

    @GetMapping("/{id}")
    Incident get(@PathVariable long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    Incident changeState(@PathVariable long id, @Valid @RequestBody ChangeStateRequest request) {
        return service.changeState(id, request.state());
    }
}
