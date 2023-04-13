package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class Onboarding2EvaluationMapper {

    public EvaluationDTO apply(OnboardingDTO onboardingDTO, InitiativeConfig initiative, List<OnboardingRejectionReason> rejectionReasons) {
        if(initiative==null
                || !initiative.isRankingInitiative()
                || (initiative.isRankingInitiative() && !rejectionReasons.isEmpty())){
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
            out.setInitiativeRewardType(initiative.getInitiativeRewardType());
            setRankingValue(onboardingDTO, initiative, out);
        }

        return out;
    }

    private RankingRequestDTO getRankingRequestDTO(OnboardingDTO onboardingDTO,InitiativeConfig initiative) {
        RankingRequestDTO out = new RankingRequestDTO();
        out.setUserId(onboardingDTO.getUserId());
        out.setInitiativeId(onboardingDTO.getInitiativeId());
        out.setOrganizationId(initiative.getOrganizationId());
        out.setAdmissibilityCheckDate(LocalDateTime.now());
        out.setCriteriaConsensusTimestamp(onboardingDTO.getCriteriaConsensusTimestamp());

        setRankingValue(onboardingDTO, initiative, out);

        out.setOnboardingKo(false);

        return out;
    }

    private static void setRankingValue(OnboardingDTO onboardingDTO, InitiativeConfig initiative, EvaluationDTO out) {
        if(initiative.isRankingInitiative() && !initiative.getRankingFields().isEmpty()){
            out.setRankingValue(initiative.getRankingFields().get(0).getFieldCode().equals(OnboardingConstants.CRITERIA_CODE_ISEE) ? Utils.euro2Cents(onboardingDTO.getIsee()) : -1);
        }
    }

    public RankingRequestDTO apply(EvaluationCompletedDTO evaluationCompletedDTO) {
        RankingRequestDTO out = new RankingRequestDTO();
        out.setUserId(evaluationCompletedDTO.getUserId());
        out.setInitiativeId(evaluationCompletedDTO.getInitiativeId());
        out.setOrganizationId(evaluationCompletedDTO.getOrganizationId());
        out.setAdmissibilityCheckDate(evaluationCompletedDTO.getAdmissibilityCheckDate());
        out.setCriteriaConsensusTimestamp(evaluationCompletedDTO.getCriteriaConsensusTimestamp());
        out.setRankingValue(Optional.ofNullable(evaluationCompletedDTO.getRankingValue()).orElse(-1L));

        out.setOnboardingKo(OnboardingConstants.ONBOARDING_STATUS_KO.equals(evaluationCompletedDTO.getStatus()));

        return out;
    }

}
