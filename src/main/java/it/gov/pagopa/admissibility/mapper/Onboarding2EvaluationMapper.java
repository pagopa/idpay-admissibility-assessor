package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.utils.CommonUtilities;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        out.setFamilyId(getFamilyId(onboardingDTO));
        out.setMemberIds(getFamilyMembers(onboardingDTO));
        out.setInitiativeId(onboardingDTO.getInitiativeId());
        out.setAdmissibilityCheckDate(LocalDateTime.now());
        out.setCriteriaConsensusTimestamp(onboardingDTO.getCriteriaConsensusTimestamp());

        if(CollectionUtils.isEmpty(rejectionReasons)){
            out.setStatus(OnboardingEvaluationStatus.ONBOARDING_OK);
        } else {
            out.setStatus(OnboardingEvaluationStatus.ONBOARDING_KO);
            out.getOnboardingRejectionReasons().addAll(rejectionReasons);
        }

        if(initiative != null){
            out.setInitiativeName(initiative.getInitiativeName());
            out.setInitiativeEndDate(initiative.getEndDate());
            out.setOrganizationId(initiative.getOrganizationId());
            out.setOrganizationName(initiative.getOrganizationName());
            out.setBeneficiaryBudgetCents(initiative.getBeneficiaryInitiativeBudgetCents());
            out.setInitiativeRewardType(initiative.getInitiativeRewardType());
            out.setIsLogoPresent(initiative.getIsLogoPresent());
            setRankingValue(onboardingDTO, initiative, out);
        }

        return out;
    }

    private RankingRequestDTO getRankingRequestDTO(OnboardingDTO onboardingDTO,InitiativeConfig initiative) {
        RankingRequestDTO out = new RankingRequestDTO();
        out.setUserId(onboardingDTO.getUserId());
        out.setFamilyId(getFamilyId(onboardingDTO));
        out.setMemberIds(getFamilyMembers(onboardingDTO));
        out.setInitiativeId(onboardingDTO.getInitiativeId());
        out.setOrganizationId(initiative.getOrganizationId());
        out.setAdmissibilityCheckDate(LocalDateTime.now());
        out.setCriteriaConsensusTimestamp(onboardingDTO.getCriteriaConsensusTimestamp());

        setRankingValue(onboardingDTO, initiative, out);

        out.setOnboardingKo(false);

        return out;
    }

    private static String getFamilyId(OnboardingDTO onboardingDTO) {
        return onboardingDTO.getFamily() != null ? onboardingDTO.getFamily().getFamilyId() : null;
    }

    private static Set<String> getFamilyMembers(OnboardingDTO onboardingDTO) {
        return onboardingDTO.getFamily() != null ? onboardingDTO.getFamily().getMemberIds() : null;
    }

    private static void setRankingValue(OnboardingDTO onboardingDTO, InitiativeConfig initiative, EvaluationDTO out) {
        if(initiative.isRankingInitiative() && !initiative.getRankingFields().isEmpty()){
            long rankingValue = -1L;
            if(initiative.getRankingFields().get(0).getFieldCode().equals(OnboardingConstants.CRITERIA_CODE_ISEE) && onboardingDTO.getIsee() != null){
                rankingValue = CommonUtilities.euroToCents(onboardingDTO.getIsee());
            }
            out.setRankingValue(rankingValue);
        }
    }

    public RankingRequestDTO apply(OnboardingDTO request, EvaluationCompletedDTO evaluationCompletedDTO) {
        RankingRequestDTO out = new RankingRequestDTO();
        out.setUserId(evaluationCompletedDTO.getUserId());
        out.setFamilyId(evaluationCompletedDTO.getFamilyId());
        out.setInitiativeId(evaluationCompletedDTO.getInitiativeId());
        out.setOrganizationId(evaluationCompletedDTO.getOrganizationId());
        out.setAdmissibilityCheckDate(evaluationCompletedDTO.getAdmissibilityCheckDate());
        out.setCriteriaConsensusTimestamp(evaluationCompletedDTO.getCriteriaConsensusTimestamp());
        out.setRankingValue(Optional.ofNullable(evaluationCompletedDTO.getRankingValue()).orElse(-1L));

        out.setOnboardingKo(
                OnboardingEvaluationStatus.ONBOARDING_KO.equals(evaluationCompletedDTO.getStatus()) ||
                        OnboardingEvaluationStatus.REJECTED.equals(evaluationCompletedDTO.getStatus()));

        if(request.getFamily() != null) {
            out.setFamilyId(request.getFamily().getFamilyId());
            out.setMemberIds(request.getFamily().getMemberIds());
        }

        return out;
    }

}
