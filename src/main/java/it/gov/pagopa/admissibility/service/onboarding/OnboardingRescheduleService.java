package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import org.springframework.messaging.Message;

import java.time.OffsetDateTime;

public interface OnboardingRescheduleService {
    void reschedule(OnboardingDTO request, OffsetDateTime rescheduleDateTime, String cause, Message<String> message);
}
