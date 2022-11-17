package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.RankingRequestDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class Evaluation2RankingRequestMapper implements Function<ImmutableTriple<EvaluationDTO, InitiativeConfig, OnboardingDTO>, RankingRequestDTO> {
    @Override
    public RankingRequestDTO apply(ImmutableTriple<EvaluationDTO, InitiativeConfig, OnboardingDTO> triple) {
        return RankingRequestDTO.builder()
                .userId(triple.left.getUserId())
                .initiativeId(triple.left.getInitiativeId())
                .initiativeName(triple.left.getInitiativeName())
                .initiativeEndDate(triple.left.getInitiativeEndDate())
                .organizationId(triple.left.getOrganizationId())
                .admissibilityCheckDate(triple.left.getAdmissibilityCheckDate())
                .onboardingRejectionReasons(triple.left.getOnboardingRejectionReasons())
                .beneficiaryBudget(triple.left.getBeneficiaryBudget())
                .serviceId(triple.left.getServiceId())
                .criteriaConsensusTimestamp(triple.left.getCriteriaConsensusTimestamp())
                .rankingValue(triple.middle.getRankingFieldCodes().get(0).equals(OnboardingConstants.CRITERIA_CODE_ISEE) ? triple.right.getIsee() : null)
                .build();
    }
}
