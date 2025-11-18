package it.gov.pagopa.admissibility.enums;

public enum OnboardingEvaluationStatus {
    /** if initiative rules are not satisfied */
    ONBOARDING_KO,
    /** if the initiative is satisfied */
    ONBOARDING_OK,
    /** In families initiatives, the status to invite other family's members */
    DEMANDED,
    /** In families initiatives, the status associated to other family's members which will ask to join to a ONBOARDING_OK family */
    JOINED,
    /** In families initiatives, the status associated to other family's members which will ask to join to a ONBOARDING_KO family */
    REJECTED,
}
