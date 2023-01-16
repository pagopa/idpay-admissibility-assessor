package it.gov.pagopa.admissibility.rest.anpr.exception;

public class AnprDailyRequestLimitException extends RuntimeException{

    public AnprDailyRequestLimitException(Throwable ex) {
        super(ex);
    }

    public AnprDailyRequestLimitException() {

    }
}
