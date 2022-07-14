package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static it.gov.pagopa.admissibility.utils.Constants.ONBOARDING_CONTEXT_INITIATIVE_KEY;

@Service
@Slf4j
@Order(1)
public class OnboardingInitiativeCheck implements OnboardingCheck{
    private final OnboardingContextHolderService onboardingContextHolderService;

    public OnboardingInitiativeCheck(OnboardingContextHolderService onboardingContextHolderService) {
        this.onboardingContextHolderService = onboardingContextHolderService;
    }

    @Override
    public String apply(OnboardingDTO onboardingDTO, Map<String, Object> onboardingContext) {
        InitiativeConfig initiativeConfig = onboardingContextHolderService.getInitiativeConfig(onboardingDTO.getInitiativeId());
        if(initiativeConfig == null){
            log.error("cannot find the initiative id %s to which the user %s is asking to onboard".formatted(onboardingDTO.getInitiativeId(), onboardingDTO.getUserId()));
            return "INVALID_INITIATIVE_ID";
        }
        onboardingContext.put(ONBOARDING_CONTEXT_INITIATIVE_KEY,initiativeConfig);

        String tcAcceptDateCheck = dateCheck(onboardingDTO.getTcAcceptTimestamp(), initiativeConfig.getStartDate(),
                initiativeConfig.getEndDate(),"CONSENSUS_CHECK_TC_ACCEPT_FAIL");
        if(tcAcceptDateCheck != null){
            return tcAcceptDateCheck;
        }

        return dateCheck(onboardingDTO.getCriteriaConsensusTimestamp(), initiativeConfig.getStartDate(),
                initiativeConfig.getEndDate(),"CONSENSUS_CHECK_CRITERIA_CONSENSUS_FAIL");
    }

    private String dateCheck (LocalDateTime dateToCheck, LocalDate startDate, LocalDate endDate,String fieldToCheck){
        if(dateToCheck.toLocalDate().isBefore(startDate) || dateToCheck.toLocalDate().isAfter(endDate)){
            return fieldToCheck;
        }
        return null;
    }
}
