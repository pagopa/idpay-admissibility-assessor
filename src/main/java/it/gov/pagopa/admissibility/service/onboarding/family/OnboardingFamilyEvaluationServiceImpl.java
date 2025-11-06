package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.connector.repository.onboarding.OnboardingRepository;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.admissibility.exception.FamilyAlreadyOnBoardingException;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.model.onboarding.Onboarding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OnboardingFamilyEvaluationServiceImpl implements OnboardingFamilyEvaluationService {

    public static final Comparator<OnboardingFamilies> COMPARATOR_FAMILIES_CREATE_DATE_DESC = Comparator.comparing(OnboardingFamilies::getCreateDate).reversed();

    private final OnboardingFamiliesRepository onboardingFamiliesRepository;
    private final ExistentFamilyHandlerService existentFamilyHandlerService;
    private final FamilyDataRetrieverFacadeService familyDataRetrieverFacadeService;

    private final OnboardingRepository onboardingRepository;

    public OnboardingFamilyEvaluationServiceImpl(OnboardingFamiliesRepository onboardingFamiliesRepository,
                                                 ExistentFamilyHandlerService existentFamilyHandlerService,
                                                 FamilyDataRetrieverFacadeService familyDataRetrieverFacadeService,
                                                 OnboardingRepository onboardingRepository) {
        this.onboardingFamiliesRepository = onboardingFamiliesRepository;
        this.existentFamilyHandlerService = existentFamilyHandlerService;
        this.familyDataRetrieverFacadeService = familyDataRetrieverFacadeService;
        this.onboardingRepository = onboardingRepository;
    }

    @Override
    public Mono<EvaluationDTO> checkOnboardingFamily(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message, boolean retrieveFamily) {
        log.debug("[ONBOARDING_REQUEST] Checking if user family has been onboarded: userId {}; initiativeId {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());

        return onboardingFamiliesRepository.findByMemberIdsInAndInitiativeId(onboardingRequest.getUserId(), onboardingRequest.getInitiativeId())
                .collectSortedList(COMPARATOR_FAMILIES_CREATE_DATE_DESC)
                .flatMap(f -> {
                    if (f.isEmpty()) {
                        return retrieveFamily ?
                                familyDataRetrieverFacadeService.retrieveFamily(onboardingRequest, initiativeConfig, message)
                                : Mono.empty();
                    } else {
                        return existentFamilyHandlerService.handleExistentFamily(onboardingRequest, f.getFirst(), initiativeConfig, message);
                    }
                });
    }

    @Override
    public Mono<EvaluationDTO> retrieveAndCheckOnboardingFamily(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message, boolean retrieveFamily) {
        log.debug("[ONBOARDING_REQUEST] Checking if user family has been onboarded: userId {}; initiativeId {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());
        return retrieveFamily ?
                familyDataRetrieverFacadeService.retrieveFamily(onboardingRequest, initiativeConfig, message)
                        .switchIfEmpty(Mono.defer(() -> checkFamilyMembers(onboardingRequest, initiativeConfig, true)))
                        .onErrorResume(FamilyAlreadyOnBoardingException.class, x -> {
                            log.info("[ONBOARDING_REQUEST][FAMILY_ALREADY_ONBOARDED] User family already onboarded for initiativeId {}", onboardingRequest.getInitiativeId());
                            return checkFamilyMembers(onboardingRequest, initiativeConfig, false);
                        })

                : Mono.empty();
    }

    private Mono<EvaluationDTO> checkFamilyMembers(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, boolean isNewFamily) {
        log.info("[ONBOARDING_REQUEST] Checking if a family member of user {} is already onboarded", onboardingRequest.getUserId());
        Set<String> onboardingIds = onboardingRequest.getFamily().getMemberIds().stream().filter(u -> !u.equals(onboardingRequest.getUserId()))
                .map(memberId -> Onboarding.buildId(onboardingRequest.getInitiativeId(), memberId)).collect(Collectors.toSet());
        return onboardingRepository.findByIdInAndStatus(onboardingIds, OnboardingEvaluationStatus.ONBOARDING_OK.name())
                .collectList()
                .flatMap(familiesOk -> {
                    if (!familiesOk.isEmpty()){
                        return existentFamilyHandlerService.mapFamilyMemberAlreadyOnboardingResult(onboardingRequest,  onboardingRequest.getFamily().getFamilyId(),  initiativeConfig)
                                .flatMap(ev -> {
                                    if(isNewFamily){
                                        return updateOnboardingFamilyOutcome(onboardingRequest.getFamily(), initiativeConfig, ev);
                                    }
                                    return Mono.just(ev);
                                });
                    } else {
                        return Mono.empty();
                    }
                });
    }

    @Override
    public Mono<EvaluationDTO> updateOnboardingFamilyOutcome(Family family, InitiativeConfig initiativeConfig, EvaluationDTO result) {
        OnboardingFamilyEvaluationStatus resultedStatus;
        List<OnboardingRejectionReason> resultedOnboardingRejectionReasons;

        if(result instanceof EvaluationCompletedDTO evaluationCompletedResult){
            resultedStatus = transcodeEvaluationStatus(evaluationCompletedResult);
            resultedOnboardingRejectionReasons = evaluationCompletedResult.getOnboardingRejectionReasons();
        } else {
            resultedStatus = OnboardingFamilyEvaluationStatus.ONBOARDING_OK;
            resultedOnboardingRejectionReasons = Collections.emptyList();
        }

        log.info("[ONBOARDING_REQUEST] Updating user family onboarding status: userId {}; familyId {}; initiativeId {}; status {}", result.getUserId(), family.getFamilyId(), initiativeConfig.getInitiativeId(), resultedStatus);
        log.info("[ONBOARDING_REQUEST_FAMILY] Updating user family: {}", family);

        return onboardingFamiliesRepository.updateOnboardingFamilyOutcome(family, initiativeConfig.getInitiativeId(), resultedStatus, resultedOnboardingRejectionReasons)
                .then(Mono.just(result));
    }

    private static OnboardingFamilyEvaluationStatus transcodeEvaluationStatus(EvaluationCompletedDTO evaluationCompletedResult) {
        return switch (evaluationCompletedResult.getStatus()) {
            case ONBOARDING_OK, JOINED, DEMANDED -> OnboardingFamilyEvaluationStatus.ONBOARDING_OK;
            case ONBOARDING_KO, REJECTED -> OnboardingFamilyEvaluationStatus.ONBOARDING_KO;
        };
    }

}
