package it.gov.pagopa.admissibility.service;

import org.springframework.messaging.Message;

public interface ErrorNotifierService {
    void notifyBeneficiaryRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyAdmissibility(Message<?> message, String description, boolean retryable, Throwable exception);
    void notify(String srcServer, String srcTopic, Message<?> message, String description, boolean retryable, Throwable exception);
}
