package it.gov.pagopa.admissibility.service.onboarding.notifier;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import org.springframework.messaging.Message;

import java.time.Instant;

public interface OnboardingRescheduleService {
    void reschedule(OnboardingDTO request, Instant rescheduleDateTime, String cause, Message<String> message);
}
