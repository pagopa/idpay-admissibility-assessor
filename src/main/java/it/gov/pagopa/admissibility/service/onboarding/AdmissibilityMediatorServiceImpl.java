package it.gov.pagopa.admissibility.service.onboarding;


import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static it.gov.pagopa.admissibility.utils.OnboardingConstants.ONBOARDING_CONTEXT_INITIATIVE_KEY;

@Service
@Slf4j
public class AdmissibilityMediatorServiceImpl implements AdmissibilityMediatorService {

    private final OnboardingCheckService onboardingCheckService;
    private final AuthoritiesDataRetrieverService authoritiesDataRetrieverService;
    private final OnboardingRequestEvaluatorService onboardingRequestEvaluatorService;
    private final Onboarding2EvaluationMapper onboarding2EvaluationMapper;

    public AdmissibilityMediatorServiceImpl(OnboardingCheckService onboardingCheckService, AuthoritiesDataRetrieverService authoritiesDataRetrieverService, OnboardingRequestEvaluatorService onboardingRequestEvaluatorService, Onboarding2EvaluationMapper onboarding2EvaluationMapper) {
        this.onboardingCheckService = onboardingCheckService;
        this.authoritiesDataRetrieverService = authoritiesDataRetrieverService;
        this.onboardingRequestEvaluatorService = onboardingRequestEvaluatorService;
        this.onboarding2EvaluationMapper = onboarding2EvaluationMapper;
    }


    /**
     * This component will take a {@link OnboardingDTO} and will calculate the {@link EvaluationDTO}
     */
    @Override
    public Flux<EvaluationDTO> execute(Flux<OnboardingDTO> onboardingDTOFlux) {
        return onboardingDTOFlux.flatMap(this::execute);
    }

    private Mono<EvaluationDTO> execute(OnboardingDTO onboardingRequest) {
        Map<String,Object> onboardingContext = new HashMap<>();

        EvaluationDTO rejectedRequest = evaluateOnboardingChecks(onboardingRequest, onboardingContext);
        if(rejectedRequest!= null){
            return Mono.just(rejectedRequest);
        } else {
            return retrieveAuthoritiesDataAndEvaluateRequest(onboardingRequest, onboardingContext);
        }
    }

    private EvaluationDTO evaluateOnboardingChecks(OnboardingDTO onboardingRequest, Map<String, Object> onboardingContext) {
        String rejectionReason = onboardingCheckService.check(onboardingRequest, onboardingContext);
        if (StringUtils.hasText(rejectionReason)) {
            log.info("[ONBOARDING_KO] Onboarding request failed: {}",rejectionReason);
            return onboarding2EvaluationMapper.apply(onboardingRequest, Collections.singletonList(rejectionReason));
        } else return null;
    }

    private Mono<EvaluationDTO> retrieveAuthoritiesDataAndEvaluateRequest(OnboardingDTO onboardingRequest, Map<String, Object> onboardingContext) {
        final InitiativeConfig initiativeConfig = (InitiativeConfig) onboardingContext.get(ONBOARDING_CONTEXT_INITIATIVE_KEY);

        return authoritiesDataRetrieverService.retrieve(onboardingRequest, initiativeConfig)
                .flatMap(r -> onboardingRequestEvaluatorService.evaluate(r, initiativeConfig));
    }

}
