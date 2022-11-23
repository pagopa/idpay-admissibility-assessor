package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@Slf4j
@Order(0)
public class OnboardingConsensusCheck implements OnboardingCheck {

    @Override
    public OnboardingRejectionReason apply(OnboardingDTO onboardingRequest, Map<String, Object> onboardingContext) {
        log.debug("[ONBOARDING_REQUEST] [ONBOARDING_CHECK] evaluating consensus check on onboarding request of user {} into initiative {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());
        final String consensusBasedRejectionReason = checkConsensusErrors(onboardingRequest);
        return StringUtils.hasText(consensusBasedRejectionReason) ?
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED)
                        .code(consensusBasedRejectionReason)
                        .build()
                : null;
    }

    private String checkConsensusErrors(OnboardingDTO onboardingRequest) {
        if (!onboardingRequest.isTc()) {
            return OnboardingConstants.REJECTION_REASON_CONSENSUS_TC_FAIL;
        }

        if (!Boolean.TRUE.equals(onboardingRequest.getPdndAccept())) { // TODO we should check from the initiative if there is a pdnd token before to invalidate if not provided?
            return OnboardingConstants.REJECTION_REASON_CONSENSUS_PDND_FAIL;
        }

        /*
        if (!CollectionUtils.isEmpty(onboardingRequest.getSelfDeclarationList())) {
            return selfDeclarationListCheck(onboardingRequest.getSelfDeclarationList());
        }
        */
        return null;
    }

    //Handle multi and boolean criteria
    /*
    private String selfDeclarationListCheck(Map<String, Boolean> selfDeclarationList) {
        for (Map.Entry<String, Boolean> selfDeclaration : selfDeclarationList.entrySet()) {
            if (Boolean.FALSE.equals(selfDeclaration.getValue())) {
                return String.format(OnboardingConstants.REJECTION_REASON_CONSENSUS_CHECK_SELF_DECLARATION_FAIL_FORMAT, selfDeclaration.getKey());
            }
        }
        return null;
    }
    */

}
