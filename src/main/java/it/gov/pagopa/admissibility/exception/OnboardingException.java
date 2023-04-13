package it.gov.pagopa.admissibility.exception;

import lombok.Getter;

@Getter
public class OnboardingException extends RuntimeException{

    private final String rejectionReason;
    private final boolean printStackTrace;

    public OnboardingException(String rejectionReason, String message){
        this(rejectionReason, message, null);
    }

    public OnboardingException(String rejectionReason, String message, Throwable ex){
        this(rejectionReason, message, false, ex);
    }

    public OnboardingException(String rejectionReason, String message, boolean printStackTrace, Throwable ex){
        super(message, ex);
        this.rejectionReason=rejectionReason;
        this.printStackTrace=printStackTrace;
    }
}
