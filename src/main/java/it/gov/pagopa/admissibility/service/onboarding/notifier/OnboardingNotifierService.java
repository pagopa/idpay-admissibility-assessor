package it.gov.pagopa.admissibility.service.onboarding.notifier;

import it.gov.pagopa.admissibility.dto.notification.NotificationQueueDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;

public interface OnboardingNotifierService {
    boolean notify(EvaluationDTO evaluationDTO);
    boolean notifyNotificationRequest(NotificationQueueDTO notificationQueueDTO);
}
