package it.gov.pagopa.common.reactive.pdnd.exception;

public class PdndRetrieveFamilyServiceTooManyRequestException extends RuntimeException {
    public PdndRetrieveFamilyServiceTooManyRequestException(String message, Throwable e){
        super(message, e);
    }
}
