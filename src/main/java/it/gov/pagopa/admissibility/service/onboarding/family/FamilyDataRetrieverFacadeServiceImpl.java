package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.FamilyDataRetrieverService;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FamilyDataRetrieverFacadeServiceImpl implements FamilyDataRetrieverFacadeService {

    private final FamilyDataRetrieverService familyDataRetrieverService;

    public FamilyDataRetrieverFacadeServiceImpl(FamilyDataRetrieverService familyDataRetrieverService) {
        this.familyDataRetrieverService = familyDataRetrieverService;
    }

    @Override
    public Mono<EvaluationDTO> retrieveFamily(OnboardingDTO onboardingRequest, Message<String> message) {
        // TODO
        return familyDataRetrieverService.retrieveFamily(onboardingRequest, message)
                .flatMap(family -> {
                    onboardingRequest.setFamily(family);
                    // TODO upsert to verify if inserted or created
                    return null;
                });
    }
}
