package it.gov.pagopa.service;

import it.gov.pagopa.dto.EvaluationDTO;
import it.gov.pagopa.dto.OnboardingDTO;
import org.springframework.stereotype.Service;

@Service
public class AdmissibilityCheckServiceImpl implements AdmissibilityCheckService {

    @Override
    public EvaluationDTO applyRules(OnboardingDTO user) {
        return EvaluationDTO.builder().userId(user.getUserId()).initiativeId(user.getInitiativeId()).status("OK").build();
    }
}
