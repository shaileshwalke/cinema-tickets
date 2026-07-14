package uk.gov.dwp.uc.pairtest.exception;

/**
 * Thrown when a ticket purchase request breaks one of the cinema's
 * business rules.
 */
public class InvalidPurchaseException extends RuntimeException {

    public InvalidPurchaseException() {
        super();
    }

    public InvalidPurchaseException(String message) {
        super(message);
    }

    public InvalidPurchaseException(String message, Throwable cause) {
        super(message, cause);
    }

}
