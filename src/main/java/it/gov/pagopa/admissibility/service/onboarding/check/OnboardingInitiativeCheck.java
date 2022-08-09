package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static it.gov.pagopa.admissibility.utils.OnboardingConstants.ONBOARDING_CONTEXT_INITIATIVE_KEY;

@Service
@Slf4j
@Order(1)
public class OnboardingInitiativeCheck implements OnboardingCheck{
    private final OnboardingContextHolderService onboardingContextHolderService;

    public OnboardingInitiativeCheck(OnboardingContextHolderService onboardingContextHolderService) {
        this.onboardingContextHolderService = onboardingContextHolderService;
    }

    private String dateCheck (LocalDateTime dateToCheck, LocalDate startDate, LocalDate endDate,String rejectionReason){
        if(dateToCheck.toLocalDate().isBefore(startDate) || (endDate!=null && dateToCheck.toLocalDate().isAfter(endDate))){
            return rejectionReason;
        }
        return null;
    }

    @Override
    public OnboardingRejectionReason apply(OnboardingDTO onboardingDTO, Map<String, Object> onboardingContext) {
        final String initiativeBasedRejectionReason = checkInitiativeError(onboardingDTO, onboardingContext);
        return StringUtils.hasText(initiativeBasedRejectionReason) ?
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                        .code(initiativeBasedRejectionReason)
                        .build()
                : null;
    }

    private String checkInitiativeError(OnboardingDTO onboardingDTO, Map<String, Object> onboardingContext) {
        InitiativeConfig initiativeConfig = onboardingContextHolderService.getInitiativeConfig(onboardingDTO.getInitiativeId());
        if(initiativeConfig == null){
            log.error("cannot find the initiative id %s to which the user %s is asking to onboard".formatted(onboardingDTO.getInitiativeId(), onboardingDTO.getUserId()));
            return OnboardingConstants.REJECTION_REASON_INVALID_INITIATIVE_ID_FAIL;
        }
        onboardingContext.put(ONBOARDING_CONTEXT_INITIATIVE_KEY,initiativeConfig);

        String tcAcceptDateCheck = dateCheck(onboardingDTO.getTcAcceptTimestamp(), initiativeConfig.getStartDate(),
                initiativeConfig.getEndDate(), OnboardingConstants.REJECTION_REASON_TC_CONSENSUS_DATETIME_FAIL);
        if(tcAcceptDateCheck != null){
            return tcAcceptDateCheck;
        }

        return dateCheck(onboardingDTO.getCriteriaConsensusTimestamp(), initiativeConfig.getStartDate(),
                initiativeConfig.getEndDate(), OnboardingConstants.REJECTION_REASON_CRITERIA_CONSENSUS_DATETIME_FAIL);
    }
}
