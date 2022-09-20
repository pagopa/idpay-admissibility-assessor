package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class Onboarding2EvaluationMapper {

    public EvaluationDTO apply(OnboardingDTO onboardingDTO, InitiativeConfig initiative, List<OnboardingRejectionReason> rejectionReasons) {
        EvaluationDTO out = new EvaluationDTO();
        out.setUserId(onboardingDTO.getUserId());
        out.setInitiativeId(onboardingDTO.getInitiativeId());
        out.setStatus(CollectionUtils.isEmpty(rejectionReasons) ? OnboardingConstants.ONBOARDING_STATUS_OK : OnboardingConstants.ONBOARDING_STATUS_KO);
        out.setAdmissibilityCheckDate(LocalDateTime.now());
        out.setOnboardingRejectionReasons(rejectionReasons);

        if(initiative!=null){
            out.setInitiativeName(initiative.getInitiativeName());
            out.setInitiativeEndDate(initiative.getEndDate());
            out.setOrganizationId(initiative.getOrganizationId());
            out.setServiceId(initiative.getServiceId());
            out.setBeneficiaryBudget(initiative.getBeneficiaryInitiativeBudget());
        }
        return out;
    }

}
