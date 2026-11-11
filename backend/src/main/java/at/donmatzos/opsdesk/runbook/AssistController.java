package at.donmatzos.opsdesk.runbook;

import at.donmatzos.opsdesk.ai.AiBackendException;
import at.donmatzos.opsdesk.ai.AssistAnswer;
import at.donmatzos.opsdesk.ai.RunbookAssistant;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assist")
class AssistController {

    private static final Logger log = LoggerFactory.getLogger(AssistController.class);

    private final RunbookAssistant assistant;

    AssistController(RunbookAssistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping
    AssistResponse ask(@Valid @RequestBody AssistRequest request) {
        AssistAnswer answer = assistant.ask(request.question());
        var sources = answer.sources().stream()
                .map(s -> new AssistSource(s.chunk().runbookId(), s.chunk().title(), s.chunk().section(),
                        s.chunk().path(), s.score()))
                .toList();
        // Never log the question: it may contain customer data.
        log.info("assist answered sources={} model={}",
                answer.sources().stream().map(s -> s.chunk().id()).toList(), answer.model());
        return new AssistResponse(answer.answer(), sources, answer.model(), answer.mock());
    }

    @ExceptionHandler(AiBackendException.class)
    ProblemDetail backendFailed(AiBackendException e) {
        log.warn("assist backend failed: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The assistant backend is unavailable; try again later");
    }
}
