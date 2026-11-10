package at.donmatzos.opsdesk.common;

/** The request is well-formed but refers to something that does not exist or is not allowed (422). */
public abstract class UnprocessableException extends RuntimeException {

    protected UnprocessableException(String message) {
        super(message);
    }
}
