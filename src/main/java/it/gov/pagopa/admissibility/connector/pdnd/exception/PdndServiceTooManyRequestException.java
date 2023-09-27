package it.gov.pagopa.admissibility.connector.pdnd.exception;

import it.gov.pagopa.admissibility.connector.pdnd.dto.PdndServiceConfig;

public class PdndServiceTooManyRequestException extends RuntimeException {
    public PdndServiceTooManyRequestException(PdndServiceConfig<?> pdndServiceConfig, Throwable e){
        super("[PDND][TOO_MANY_REQUEST] Pdnd service has bee invoked too times: " + pdndServiceConfig.getAudience(), e);
    }
}
