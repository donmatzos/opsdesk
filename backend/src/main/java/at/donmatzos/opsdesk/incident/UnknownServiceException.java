package at.donmatzos.opsdesk.incident;

import at.donmatzos.opsdesk.common.UnprocessableException;

/** The request refers to a service that does not exist (422, unlike a missing path resource, which is 404). */
public class UnknownServiceException extends UnprocessableException {

    public UnknownServiceException(long serviceId) {
        super("Service " + serviceId + " does not exist");
    }
}
