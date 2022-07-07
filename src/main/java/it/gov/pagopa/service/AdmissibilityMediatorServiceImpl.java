package it.gov.pagopa.service;

import it.gov.pagopa.dto.EvaluationDTO;
import it.gov.pagopa.dto.OnboardingDTO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AdmissibilityMediatorServiceImpl implements AdmissibilityMediatorService {

    private final AdmissibilityCheckService admissibilityCheckService;

    public AdmissibilityMediatorServiceImpl(AdmissibilityCheckService admissibilityCheckService) {
        this.admissibilityCheckService = admissibilityCheckService;
    }


    /**
     * This component will take a {@link OnboardingDTO} and will calculate the {@link EvaluationDTO}
     */
    @Override
    public Flux<EvaluationDTO> execute(Flux<OnboardingDTO> onboardingDTOFlux) {

        return onboardingDTOFlux.map(this.admissibilityCheckService::applyRules);
    }
}
