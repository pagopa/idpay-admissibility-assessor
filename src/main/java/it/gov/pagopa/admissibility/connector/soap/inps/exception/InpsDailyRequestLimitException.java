package it.gov.pagopa.admissibility.connector.soap.inps.exception;

public class InpsDailyRequestLimitException  extends RuntimeException {
    public InpsDailyRequestLimitException(Throwable ex) {
        super(ex);
    }
}