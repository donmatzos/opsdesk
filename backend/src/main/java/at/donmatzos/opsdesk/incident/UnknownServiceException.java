package at.donmatzos.opsdesk.incident;

/** The request refers to a service that does not exist (422, unlike a missing path resource, which is 404). */
public class UnknownServiceException extends RuntimeException {

    public UnknownServiceException(long serviceId) {
        super("Service " + serviceId + " does not exist");
    }
}
