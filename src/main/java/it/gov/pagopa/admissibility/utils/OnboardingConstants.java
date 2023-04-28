package it.gov.pagopa.admissibility.utils;

public final class OnboardingConstants {
    private OnboardingConstants(){}

    //region onboarding context keys
    public static final String ONBOARDING_CONTEXT_INITIATIVE_KEY = "INITIATIVE_KEY";
    //endregion

    //region rejection reasons
    public static final String REJECTION_REASON_CONSENSUS_TC_FAIL = "CONSENSUS_CHECK_TC_FAIL";
    public static final String REJECTION_REASON_INVALID_INITIATIVE_ID_FAIL = "INVALID_INITIATIVE_ID";
    public static final String REJECTION_REASON_TC_CONSENSUS_DATETIME_FAIL = "CONSENSUS_CHECK_TC_ACCEPT_FAIL";
    public static final String REJECTION_REASON_CRITERIA_CONSENSUS_DATETIME_FAIL = "CONSENSUS_CHECK_CRITERIA_CONSENSUS_FAIL";
    public static final String REJECTION_REASON_INITIATIVE_BUDGET_EXHAUSTED = "INITIATIVE_BUDGET_EXHAUSTED";
    public static final String REJECTION_REASON_CONSENSUS_CHECK_SELF_DECLARATION_FAIL_FORMAT = "CONSENSUS_CHECK_SELF_DECLARATION_%s_FAIL";
    public static final String REJECTION_REASON_CONSENSUS_PDND_FAIL = "CONSENSUS_CHECK_PDND_FAIL";
    public static final String REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT = "AUTOMATED_CRITERIA_%s_FAIL";
    public static final String REJECTION_REASON_ISEE_TYPE_KO = "ISEE_TYPE_FAIL";
    public static final String REJECTION_REASON_FAMILY_KO = "FAMILY_FAIL";
    public static final String REJECTION_REASON_GENERIC_ERROR = "GENERIC_ERROR";
    //endregion

    //region criteria code
    public static final String CRITERIA_CODE_ISEE = "ISEE";
    public static final String CRITERIA_CODE_FAMILY = "FAMILY";
    public static final String CRITERIA_CODE_BIRTHDATE = "BIRTHDATE";
    public static final String CRITERIA_CODE_RESIDENCE = "RESIDENCE";
    //endregion
}
