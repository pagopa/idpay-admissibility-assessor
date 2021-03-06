package it.gov.pagopa.dto.onboarding.mapper;

import it.gov.pagopa.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.dto.onboarding.OnboardingDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;

@Service
public class Onboarding2EvaluationMapper implements BiFunction<OnboardingDTO, List<String>,EvaluationDTO> {

    @Override
    public EvaluationDTO apply(OnboardingDTO onboardingDTO, List<String> rejectionReasons) {
        EvaluationDTO out = new EvaluationDTO();
        out.setUserId(onboardingDTO.getUserId());
        out.setInitiativeId(onboardingDTO.getInitiativeId());
        out.setStatus(CollectionUtils.isEmpty(rejectionReasons) ? "ONBOARDING_OK" : "ONBOARDING_KO");
        out.setAdmissibilityCheckDate(LocalDateTime.now());
        out.setOnboardingRejectionReasons(rejectionReasons);
        return out;
    }
}
