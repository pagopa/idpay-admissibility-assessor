package it.gov.pagopa.admissibility.service;

import org.springframework.messaging.Message;

public interface AdmissibilityErrorNotifierService {
    void notifyBeneficiaryRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyAdmissibility(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyAdmissibilityOutcome(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyRankingRequest(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyAdmissibilityCommands(Message<String> message, String description, boolean retryable, Throwable exception);

    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    void notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception);
}
