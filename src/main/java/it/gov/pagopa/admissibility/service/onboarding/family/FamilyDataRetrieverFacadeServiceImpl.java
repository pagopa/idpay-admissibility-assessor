package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.FamilyDataRetrieverService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Service
public class FamilyDataRetrieverFacadeServiceImpl implements FamilyDataRetrieverFacadeService {

    private final FamilyDataRetrieverService familyDataRetrieverService;
    private final OnboardingFamiliesRepository repository;
    private final ExistentFamilyHandlerService existentFamilyHandlerService;
    private final CriteriaCodeService criteriaCodeService;
    private final Onboarding2EvaluationMapper evaluationMapper;

    public FamilyDataRetrieverFacadeServiceImpl(FamilyDataRetrieverService familyDataRetrieverService, OnboardingFamiliesRepository repository, ExistentFamilyHandlerService existentFamilyHandlerService, CriteriaCodeService criteriaCodeService, Onboarding2EvaluationMapper evaluationMapper) {
        this.familyDataRetrieverService = familyDataRetrieverService;
        this.repository = repository;
        this.existentFamilyHandlerService = existentFamilyHandlerService;
        this.criteriaCodeService = criteriaCodeService;
        this.evaluationMapper = evaluationMapper;
    }

    @Override
    public Mono<EvaluationDTO> retrieveFamily(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message) {
        log.info("[ONBOARDING_REQUEST] User family NOT already onboarded, retrieving it: userId {}; initiativeId {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());

        Mono<Optional<Family>> familyRetrieverMono = onboardingRequest.getFamily() != null
                ? Mono.just(Optional.of(onboardingRequest.getFamily()))
                : (familyDataRetrieverService.retrieveFamily(onboardingRequest, message,initiativeConfig.getInitiativeName(),initiativeConfig.getOrganizationName())
                        .switchIfEmpty(Mono.just(Optional.empty())));

        return familyRetrieverMono
                .flatMap(familyOpt -> {
                    if (familyOpt.isPresent()) {
                        Family family = familyOpt.get();
                        onboardingRequest.setFamily(family);

                        return repository.createIfNotExistsInProgressFamilyOnboardingOrReturnEmpty(family, onboardingRequest.getInitiativeId())
                                .map((Function<? super OnboardingFamilies, EvaluationDTO>) x -> {
                                    throw new FamilyOnboardingRequestCreated();
                                })
                                .switchIfEmpty(Mono.defer(() -> handleCreateConflict(onboardingRequest, initiativeConfig, message, family)))

                                .onErrorResume(FamilyOnboardingRequestCreated.class, x -> {
                                    log.info("[ONBOARDING_REQUEST] User family onboarding request created by {}: familyId {}; initiativeId {}", onboardingRequest.getUserId(), family.getFamilyId(), onboardingRequest.getInitiativeId());
                                    log.info("[ONBOARDING_REQUEST_FAMILY] User family onboarding request created: {}", family);

                                    return Mono.empty();
                                });
                    } else {
                        EvaluationDTO evaluationKo = evaluationMapper.apply(onboardingRequest, initiativeConfig, List.of(buildFamilyNotAvailableRejectionReason()));
                        return Mono.just(evaluationKo);
                    }
                });
    }

    private OnboardingRejectionReason buildFamilyNotAvailableRejectionReason() {
        CriteriaCodeConfig criteriaCodeConfig = criteriaCodeService.getCriteriaCodeConfig(OnboardingConstants.CRITERIA_CODE_FAMILY);
        return new OnboardingRejectionReason(OnboardingRejectionReason.OnboardingRejectionReasonType.FAMILY_KO, OnboardingConstants.REJECTION_REASON_FAMILY_KO, criteriaCodeConfig.getAuthority(), criteriaCodeConfig.getAuthorityLabel(), "Nucleo familiare non disponibile");
    }

    private static class FamilyOnboardingRequestCreated extends RuntimeException {
    }

    private Mono<EvaluationDTO> handleCreateConflict(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message, Family family) {
        return repository.findById(OnboardingFamilies.buildId(family, onboardingRequest.getInitiativeId()))
                .flatMap(previousFamilyRequest -> existentFamilyHandlerService.handleExistentFamily(onboardingRequest, previousFamilyRequest, initiativeConfig, message));
    }
}
