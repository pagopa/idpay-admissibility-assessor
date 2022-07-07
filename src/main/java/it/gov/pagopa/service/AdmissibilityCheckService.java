package it.gov.pagopa.service;

import it.gov.pagopa.dto.EvaluationDTO;
import it.gov.pagopa.dto.OnboardingDTO;

public interface AdmissibilityCheckService {

    EvaluationDTO applyRules(OnboardingDTO user);
}
