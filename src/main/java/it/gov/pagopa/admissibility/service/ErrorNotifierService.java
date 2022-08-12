package it.gov.pagopa.admissibility.service;

public interface ErrorNotifierService {
    void notifyBeneficiaryRuleBuilder(Object payload, String description, boolean retryable, Throwable exception);
    void notifyAdmissibility(Object payload, String description, boolean retryable, Throwable exception);
    void notify(String srcServer, String srcTopic, Object payload, String description, boolean retryable, Throwable exception);
}
