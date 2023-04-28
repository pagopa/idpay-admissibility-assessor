package it.gov.pagopa.admissibility.exception;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import lombok.Getter;

import java.util.List;

@Getter
public class OnboardingException extends RuntimeException{

    private final List<OnboardingRejectionReason> rejectionReasons;
    private final boolean printStackTrace;

    public OnboardingException(List<OnboardingRejectionReason> rejectionReasons, String message){
        this(rejectionReasons, message, null);
    }

    public OnboardingException(List<OnboardingRejectionReason> rejectionReasons, String message, Throwable ex){
        this(rejectionReasons, message, false, ex);
    }

    public OnboardingException(List<OnboardingRejectionReason> rejectionReasons, String message, boolean printStackTrace, Throwable ex){
        super(message, ex);
        this.rejectionReasons = rejectionReasons;
        this.printStackTrace=printStackTrace;
    }
}
