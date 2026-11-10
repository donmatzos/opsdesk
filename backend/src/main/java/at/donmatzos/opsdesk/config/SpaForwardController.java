package at.donmatzos.opsdesk.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Angular routes are client-side; a browser refresh on one hits the server. Forward exactly the known routes to
 * index.html (keep in sync with frontend/src/app/app.routes.ts), so unknown paths still return a 404.
 */
@Controller
class SpaForwardController {

    @GetMapping({"/services", "/incidents", "/incidents/{id}"})
    String forwardToIndex() {
        return "forward:/index.html";
    }
}
