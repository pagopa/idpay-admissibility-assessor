package it.gov.pagopa.admissibility.service.onboarding.family;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.repository.OnboardingFamiliesRepository;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Comparator;

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
    public Mono<EvaluationDTO> checkOnboardingFamily(OnboardingDTO onboardingRequest, Message<String> message) {
        return onboardingFamiliesRepository.findByMemberIdsIn(onboardingRequest.getUserId())
                .collectSortedList(COMPARATOR_FAMILIES_CREATE_DATE_DESC)
                .flatMap(f -> {
                    if (f.isEmpty()) {
                        return familyDataRetrieverFacadeService.retrieveFamily(onboardingRequest, message);
                    } else {
                        return existentFamilyHandlerService.handleExistentFamily(onboardingRequest, f.get(0), message);
                    }
                });
    }

}
