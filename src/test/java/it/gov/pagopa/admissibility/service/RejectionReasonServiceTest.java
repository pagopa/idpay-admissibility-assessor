package it.gov.pagopa.admissibility.service;


import it.gov.pagopa.admissibility.config.CriteriaCodeConfigs;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RejectionReasonServiceTest {

    private RejectionReasonService rejectionReasonService;

    @BeforeEach
    void setUp() {
        CriteriaCodeConfigs criteriaCodeConfigs = new CriteriaCodeConfigs();

        // isee
        CriteriaCodeConfigs.CriteriaConfig iseeConfig =
                new CriteriaCodeConfigs.CriteriaConfig();
        iseeConfig.setAuthority("INPS");
        iseeConfig.setAuthorityLabel("Istituto Nazionale Previdenza Sociale");

        // residence
        CriteriaCodeConfigs.CriteriaConfig residenceConfig =
                new CriteriaCodeConfigs.CriteriaConfig();
        residenceConfig.setAuthority("AGID");
        residenceConfig.setAuthorityLabel("Agenzia per l'Italia Digitale");

        // birthdate
        CriteriaCodeConfigs.CriteriaConfig birthdateConfig =
                new CriteriaCodeConfigs.CriteriaConfig();
        birthdateConfig.setAuthority("AGID");
        birthdateConfig.setAuthorityLabel("Agenzia per l'Italia Digitale");

        criteriaCodeConfigs.getConfigs().put("isee", iseeConfig);
        criteriaCodeConfigs.getConfigs().put("residence", residenceConfig);
        criteriaCodeConfigs.getConfigs().put("birthdate", birthdateConfig);

        rejectionReasonService = new RejectionReasonService(criteriaCodeConfigs);
    }

    @Test
    void rejectionFor_Isee() {

        OnboardingRejectionReason rejection =
                rejectionReasonService.rejectionFor("isee");

        Assertions.assertEquals(
                OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                rejection.getType()
        );
        Assertions.assertEquals(
                OnboardingConstants.REJECTION_REASON_ISEE_TYPE_KO,
                rejection.getCode()
        );
        Assertions.assertEquals("INPS", rejection.getAuthority());
        Assertions.assertEquals(
                "Istituto Nazionale Previdenza Sociale",
                rejection.getAuthorityLabel()
        );
    }

    @Test
    void rejectionFor_Residence() {

        OnboardingRejectionReason rejection =
                rejectionReasonService.rejectionFor("residence");

        Assertions.assertEquals(
                OnboardingRejectionReason.OnboardingRejectionReasonType.RESIDENCE_KO,
                rejection.getType()
        );
        Assertions.assertEquals(
                OnboardingConstants.REJECTION_REASON_RESIDENCE_KO,
                rejection.getCode()
        );
        Assertions.assertEquals("AGID", rejection.getAuthority());
    }

    @Test
    void rejectionFor_Birthdate() {

        OnboardingRejectionReason rejection =
                rejectionReasonService.rejectionFor("birthdate");

        Assertions.assertEquals(
                OnboardingRejectionReason.OnboardingRejectionReasonType.BIRTHDATE_KO,
                rejection.getType()
        );
        Assertions.assertEquals(
                OnboardingConstants.REJECTION_REASON_BIRTHDATE_KO,
                rejection.getCode()
        );
        Assertions.assertEquals("AGID", rejection.getAuthority());
    }

    @Test
    void rejectionFor_UnknownCode() {

        String unknownCode = "UNKNOWN_CODE";

        OnboardingRejectionReason rejection =
                rejectionReasonService.rejectionFor(unknownCode);

        Assertions.assertEquals(
                OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL,
                rejection.getType()
        );
        Assertions.assertEquals(
                OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted(unknownCode),
                rejection.getCode()
        );
        Assertions.assertNull(rejection.getAuthority());
        Assertions.assertNull(rejection.getAuthorityLabel());
    }
}
