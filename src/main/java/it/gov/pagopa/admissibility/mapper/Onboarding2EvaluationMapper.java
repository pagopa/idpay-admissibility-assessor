package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class Onboarding2EvaluationMapper {

    public EvaluationDTO apply(OnboardingDTO onboardingDTO, InitiativeConfig initiative, List<OnboardingRejectionReason> rejectionReasons) {
        if(initiative==null
                || !Boolean.TRUE.equals(initiative.getRankingInitiative())
                || (Boolean.TRUE.equals(initiative.getRankingInitiative()) && rejectionReasons != null && !rejectionReasons.isEmpty())){
            return getEvaluationCompletedDTO(onboardingDTO, initiative, rejectionReasons);
        } else {
            return getRankingRequestDTO(onboardingDTO, initiative);
        }
    }

    private EvaluationCompletedDTO getEvaluationCompletedDTO(OnboardingDTO onboardingDTO, InitiativeConfig initiative, List<OnboardingRejectionReason> rejectionReasons) {
        EvaluationCompletedDTO out = new EvaluationCompletedDTO();
        out.setUserId(onboardingDTO.getUserId());
        out.setInitiativeId(onboardingDTO.getInitiativeId());
        out.setStatus(CollectionUtils.isEmpty(rejectionReasons) ? OnboardingConstants.ONBOARDING_STATUS_OK : OnboardingConstants.ONBOARDING_STATUS_KO);
        out.setAdmissibilityCheckDate(LocalDateTime.now());
        out.setOnboardingRejectionReasons(rejectionReasons);
        out.setCriteriaConsensusTimestamp(onboardingDTO.getCriteriaConsensusTimestamp());

        if(initiative != null){
            out.setInitiativeName(initiative.getInitiativeName());
            out.setInitiativeEndDate(initiative.getEndDate());
            out.setOrganizationId(initiative.getOrganizationId());
            out.setBeneficiaryBudget(initiative.getBeneficiaryInitiativeBudget());
        }
        return out;
    }

    private RankingRequestDTO getRankingRequestDTO(OnboardingDTO onboardingDTO,InitiativeConfig initiative) {
        RankingRequestDTO out = new RankingRequestDTO();
        out.setUserId(onboardingDTO.getUserId());
        out.setInitiativeId(onboardingDTO.getInitiativeId());
        out.setAdmissibilityCheckDate(LocalDateTime.now());
        out.setCriteriaConsensusTimestamp(onboardingDTO.getCriteriaConsensusTimestamp());
        out.setRankingValue(initiative.getRankingFieldCodes().get(0).equals(OnboardingConstants.CRITERIA_CODE_ISEE) ? onboardingDTO.getIsee() : null);
        return out;
    }

}
