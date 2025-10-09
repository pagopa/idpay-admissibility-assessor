package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class OnboardingFamilyEvaluationServiceImpl implements OnboardingFamilyEvaluationService {

    public static final Comparator<OnboardingFamilies> COMPARATOR_FAMILIES_CREATE_DATE_DESC = Comparator.comparing(OnboardingFamilies::getCreateDate).reversed();

    private final OnboardingFamiliesRepository onboardingFamiliesRepository;
    private final ExistentFamilyHandlerService existentFamilyHandlerService;
    private final FamilyDataRetrieverFacadeService familyDataRetrieverFacadeService;

    public OnboardingFamilyEvaluationServiceImpl(OnboardingFamiliesRepository onboardingFamiliesRepository, ExistentFamilyHandlerService existentFamilyHandlerService, FamilyDataRetrieverFacadeService familyDataRetrieverFacadeService) {
        this.onboardingFamiliesRepository = onboardingFamiliesRepository;
        this.existentFamilyHandlerService = existentFamilyHandlerService;
        this.familyDataRetrieverFacadeService = familyDataRetrieverFacadeService;
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
                        return existentFamilyHandlerService.handleExistentFamily(onboardingRequest, f.get(0), initiativeConfig, message);
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
