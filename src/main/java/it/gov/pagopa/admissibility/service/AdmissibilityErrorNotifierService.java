package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.config.KafkaConfiguration;
import org.springframework.messaging.Message;

public interface AdmissibilityErrorNotifierService {
    void notifyBeneficiaryRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyAdmissibility(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyAdmissibilityOutcome(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyRankingRequest(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyAdmissibilityCommands(Message<String> message, String description, boolean retryable, Throwable exception);
    void notify(KafkaConfiguration.BaseKafkaInfoDTO baseKafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception);
}
