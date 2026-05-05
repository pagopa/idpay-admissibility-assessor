package it.gov.pagopa.admissibility.exception;

public class OnboardingRequestRetryException extends RuntimeException {
    public OnboardingRequestRetryException(String message) {
        super(message);
    }
}
