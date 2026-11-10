package at.donmatzos.opsdesk.common;

import at.donmatzos.opsdesk.incident.IllegalStateTransitionException;
import at.donmatzos.opsdesk.incident.UnknownServiceException;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps domain exceptions to RFC 9457 problem details. Spring's own MVC errors (unreadable JSON, type mismatch,
 * unknown route) are handled by the superclass; validation errors additionally list the failing fields.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalStateTransitionException.class)
    ProblemDetail conflict(IllegalStateTransitionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(UnknownServiceException.class)
    ProblemDetail unprocessable(UnknownServiceException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail body = e.getBody();
        body.setProperty("errors", errors);
        return handleExceptionInternal(e, body, headers, status, request);
    }
}
