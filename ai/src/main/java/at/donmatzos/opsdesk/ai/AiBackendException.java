package at.donmatzos.opsdesk.ai;

/** A model or search backend failed. The message never contains response bodies or credentials. */
public class AiBackendException extends RuntimeException {

    public AiBackendException(String message) {
        super(message);
    }

    public AiBackendException(String message, Throwable cause) {
        super(message, cause);
    }
}
