package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@Order(0)
public class OnboardingConsensusCheck implements OnboardingCheck {

    @Override
    public OnboardingRejectionReason apply(OnboardingDTO onboardingDTO, Map<String, Object> onboardingContext) {
        final String consensusBasedRejectionReason = checkConsensusErrors(onboardingDTO);
        return StringUtils.hasText(consensusBasedRejectionReason) ?
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED)
                        .code(consensusBasedRejectionReason)
                        .build()
                : null;
    }

    private String checkConsensusErrors(OnboardingDTO onboardingDTO) {
        if (!onboardingDTO.isTc()) {
            return OnboardingConstants.REJECTION_REASON_CONSENSUS_TC_FAIL;
        }

        if (!Boolean.TRUE.equals(onboardingDTO.getPdndAccept())) { // TODO we should check from the initiative if there is a pdnd token before to invalidate if not provided?
            return OnboardingConstants.REJECTION_REASON_CONSENSUS_PDND_FAIL;
        }

        if (onboardingDTO.getSelfDeclarationList().size() != 0 || onboardingDTO.getSelfDeclarationList() != null) {
            String declarations = selfDeclarationListCheck(onboardingDTO.getSelfDeclarationList());
            if (declarations != null) {
                return declarations;
            }
        }
        return null;
    }

    private String selfDeclarationListCheck(Map<String, Boolean> selfDeclarationList) {
        for (Map.Entry<String, Boolean> selfDeclaration : selfDeclarationList.entrySet()) {
            if (Boolean.FALSE.equals(selfDeclaration.getValue())) {
                return String.format(OnboardingConstants.REJECTION_REASON_CONSENSUS_CHECK_SELF_DECLARATION_FAIL_FORMAT, selfDeclaration.getKey());
            }
        }
        return null;
    }

}
