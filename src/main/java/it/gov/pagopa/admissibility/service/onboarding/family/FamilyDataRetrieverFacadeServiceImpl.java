package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.FamilyDataRetrieverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Slf4j
@Service
public class FamilyDataRetrieverFacadeServiceImpl implements FamilyDataRetrieverFacadeService {

    private final FamilyDataRetrieverService familyDataRetrieverService;
    private final OnboardingFamiliesRepository repository;
    private final ExistentFamilyHandlerService existentFamilyHandlerService;

    public FamilyDataRetrieverFacadeServiceImpl(FamilyDataRetrieverService familyDataRetrieverService, OnboardingFamiliesRepository repository, ExistentFamilyHandlerService existentFamilyHandlerService) {
        this.familyDataRetrieverService = familyDataRetrieverService;
        this.repository = repository;
        this.existentFamilyHandlerService = existentFamilyHandlerService;
    }

    @Override
    public Mono<EvaluationDTO> retrieveFamily(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message) {
        log.info("[ONBOARDING_REQUEST] User family NOT already onboarded, retrieving it: userId {}; initiativeId {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());

        return familyDataRetrieverService.retrieveFamily(onboardingRequest, message)
                .flatMap(family -> {
                    onboardingRequest.setFamily(family);

                    return repository.createIfNotExistsInProgressFamilyOnboardingOrReturnEmpty(family, onboardingRequest.getInitiativeId())
                            .map((Function<? super OnboardingFamilies, EvaluationDTO>)  x -> {throw new FamilyOnboardingRequestCreated();})
                            .switchIfEmpty(Mono.defer(() -> handleCreateConflict(onboardingRequest, initiativeConfig, message, family)))

                            .onErrorResume(FamilyOnboardingRequestCreated.class, x -> {
                                log.info("[ONBOARDING_REQUEST] User family onboarding request created by {}: familyId {}; initiativeId {}", onboardingRequest.getUserId(), family.getFamilyId(), onboardingRequest.getInitiativeId());

                                return Mono.empty();
                            });
                });
    }

    private static class FamilyOnboardingRequestCreated extends RuntimeException {}

    private Mono<EvaluationDTO> handleCreateConflict(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message, Family family) {
        return repository.findById(OnboardingFamilies.buildId(family, onboardingRequest.getInitiativeId()))
                .flatMap(previousFamilyRequest -> existentFamilyHandlerService.handleExistentFamily(onboardingRequest, previousFamilyRequest, initiativeConfig, message));
    }
}
