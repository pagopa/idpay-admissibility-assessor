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
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

@Service
@Slf4j
@Order(1)
public class OnboardingInitiativeCheck implements OnboardingCheck{


    private String dateCheck(
                Instant dateToCheck,
                Instant startDate,
                Instant endDate,
                String rejectionReason
        ) {
            ZoneId zone = ZoneId.of("Europe/Rome");
        
            LocalDate check = toLocalDate(dateToCheck, zone);
            LocalDate start = toLocalDate(startDate, zone);
            LocalDate end   = endDate != null ? toLocalDate(endDate, zone) : null;
        
            boolean outOfRange =
                    check.isBefore(start) ||
                    (end != null && check.isAfter(end));
        
            return outOfRange ? rejectionReason : null;
        }
        
        private LocalDate toLocalDate(Instant instant, ZoneId zone) {
            return instant.atZone(zone).toLocalDate();
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
