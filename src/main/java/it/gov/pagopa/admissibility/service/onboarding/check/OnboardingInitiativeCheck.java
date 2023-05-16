package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@Order(1)
public class OnboardingInitiativeCheck implements OnboardingCheck{

    private String dateCheck (LocalDateTime dateToCheck, LocalDate startDate, LocalDate endDate,String rejectionReason){
        if(dateToCheck.toLocalDate().isBefore(startDate) || (endDate!=null && dateToCheck.toLocalDate().isAfter(endDate))){
            return rejectionReason;
        }
        return null;
    }

    @Override
    public OnboardingRejectionReason apply(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Map<String, Object> onboardingContext) {
        log.debug("[ONBOARDING_REQUEST] [ONBOARDING_CHECK] evaluating initiative check on onboarding request of user {} into initiative {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());
        final String initiativeBasedRejectionReason = checkInitiativeError(onboardingRequest, initiativeConfig);
        return StringUtils.hasText(initiativeBasedRejectionReason) ?
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                        .code(initiativeBasedRejectionReason)
                        .build()
                : null;
    }

    private String checkInitiativeError(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig) {
        if(initiativeConfig == null){
            log.info("[ONBOARDING_REQUEST] [ONBOARDING_CHECK] [INITIATIVE_CHECK] cannot find the initiative id {} to which the user {} is asking to onboard", onboardingRequest.getInitiativeId(), onboardingRequest.getUserId());
            return OnboardingConstants.REJECTION_REASON_INVALID_INITIATIVE_ID_FAIL;
        }

        String tcAcceptDateCheck = dateCheck(onboardingRequest.getTcAcceptTimestamp(), initiativeConfig.getStartDate(),
                initiativeConfig.getEndDate(), OnboardingConstants.REJECTION_REASON_TC_CONSENSUS_DATETIME_FAIL);
        if(tcAcceptDateCheck != null){
            return tcAcceptDateCheck;
        }

        return dateCheck(onboardingRequest.getCriteriaConsensusTimestamp(), initiativeConfig.getStartDate(),
                initiativeConfig.getEndDate(), OnboardingConstants.REJECTION_REASON_CRITERIA_CONSENSUS_DATETIME_FAIL);
    }
}
