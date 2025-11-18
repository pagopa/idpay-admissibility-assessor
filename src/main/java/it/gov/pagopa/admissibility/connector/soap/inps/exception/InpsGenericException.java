package it.gov.pagopa.admissibility.connector.soap.inps.exception;

public class InpsGenericException extends RuntimeException {
    public InpsGenericException(String message, Throwable ex) {
        super(message, ex);
    }
}