package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.config.CriteriaCodeConfigs;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RejectionReasonService {
    private final CriteriaCodeConfigs criteriaConfigs;
    private final Map<String, OnboardingRejectionReason> rejectionByCode;
    public RejectionReasonService(CriteriaCodeConfigs criteriaConfigs) {
        this.criteriaConfigs = criteriaConfigs;
        this.rejectionByCode = buildRejectionMap();
    }
    private Map<String, OnboardingRejectionReason> buildRejectionMap() {
        Map<String, OnboardingRejectionReason> map = new HashMap<>();
        criteriaConfigs.getConfigs().forEach((code, cfg) -> {
            OnboardingRejectionReason.OnboardingRejectionReasonType type =
                    switch (code) {
                        case "isee" -> OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO;
                        case "residence" -> OnboardingRejectionReason.OnboardingRejectionReasonType.RESIDENCE_KO;
                        case "birthdate" -> OnboardingRejectionReason.OnboardingRejectionReasonType.BIRTHDATE_KO;
                        case "family" -> OnboardingRejectionReason.OnboardingRejectionReasonType.FAMILY_KO;
                        default -> OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL;
                    };
            String rejectionCode =
                    switch (code) {
                        case "isee" -> OnboardingConstants.REJECTION_REASON_ISEE_TYPE_KO;
                        case "residence" -> OnboardingConstants.REJECTION_REASON_RESIDENCE_KO;
                        case "birthdate" -> OnboardingConstants.REJECTION_REASON_BIRTHDATE_KO;
                        case "family" -> OnboardingConstants.REJECTION_REASON_FAMILY_KO;
                        default -> OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted(code);
                    };
            map.put(code, new OnboardingRejectionReason(
                    type,
                    rejectionCode,
                    cfg.getAuthority(),
                    cfg.getAuthorityLabel(),
                    null
            ));
        });
        return map;
    }
    public OnboardingRejectionReason rejectionFor(String code) {
        return rejectionByCode.getOrDefault(
                code,
                new OnboardingRejectionReason(
                        OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL,
                        OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted(code),
                        null, null, null
                )
        );
    }
}