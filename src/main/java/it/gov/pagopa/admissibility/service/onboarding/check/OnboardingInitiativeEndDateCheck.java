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
import java.util.Map;

@Service
@Slf4j
@Order(2)
public class OnboardingInitiativeEndDateCheck implements OnboardingCheck{

    @Override
    public OnboardingRejectionReason apply(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Map<String, Object> onboardingContext) {
        log.debug("[ONBOARDING_REQUEST] [ONBOARDING_CHECK] evaluating of the initiative end date control on the onboarding request of user {} in the initiative {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());
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

        if(!LocalDate.now().isBefore(initiativeConfig.getEndDate())) return OnboardingConstants.REJECTION_REASON_INITIATIVE_ENDED;

        return null;
    }
}
