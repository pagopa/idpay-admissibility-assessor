package it.gov.pagopa.admissibility.service;


import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.AuthoritiesDataRetrieverService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingCheckService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingRequestEvaluatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static it.gov.pagopa.admissibility.utils.OnboardingConstants.ONBOARDING_CONTEXT_INITIATIVE_KEY;

@Service
@Slf4j
public class AdmissibilityEvaluatorMediatorServiceImpl implements AdmissibilityEvaluatorMediatorService {

    private final OnboardingCheckService onboardingCheckService;
    private final AuthoritiesDataRetrieverService authoritiesDataRetrieverService;
    private final OnboardingRequestEvaluatorService onboardingRequestEvaluatorService;
    private final Onboarding2EvaluationMapper onboarding2EvaluationMapper;

    public AdmissibilityEvaluatorMediatorServiceImpl(OnboardingCheckService onboardingCheckService, AuthoritiesDataRetrieverService authoritiesDataRetrieverService, OnboardingRequestEvaluatorService onboardingRequestEvaluatorService, Onboarding2EvaluationMapper onboarding2EvaluationMapper) {
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
        OnboardingRejectionReason rejectionReason = onboardingCheckService.check(onboardingRequest, onboardingContext);
        if (rejectionReason != null) {
            log.info("[ONBOARDING_KO] Onboarding request failed: {}",rejectionReason);
            return onboarding2EvaluationMapper.apply(onboardingRequest, readInitiativeConfigFromContext(onboardingContext), Collections.singletonList(rejectionReason));
        } else return null;
    }

    private Mono<EvaluationDTO> retrieveAuthoritiesDataAndEvaluateRequest(OnboardingDTO onboardingRequest, Map<String, Object> onboardingContext) {
        final InitiativeConfig initiativeConfig = readInitiativeConfigFromContext(onboardingContext);

        return authoritiesDataRetrieverService.retrieve(onboardingRequest, initiativeConfig)
                .flatMap(r -> onboardingRequestEvaluatorService.evaluate(r, initiativeConfig));
    }

    private InitiativeConfig readInitiativeConfigFromContext(Map<String, Object> onboardingContext) {
        return (InitiativeConfig) onboardingContext.get(ONBOARDING_CONTEXT_INITIATIVE_KEY);
    }

}