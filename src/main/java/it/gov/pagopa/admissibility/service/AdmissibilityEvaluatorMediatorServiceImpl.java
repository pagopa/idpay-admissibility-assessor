package it.gov.pagopa.admissibility.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.AuthoritiesDataRetrieverService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingCheckService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingRequestEvaluatorService;
import it.gov.pagopa.admissibility.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
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
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    public AdmissibilityEvaluatorMediatorServiceImpl(OnboardingCheckService onboardingCheckService, AuthoritiesDataRetrieverService authoritiesDataRetrieverService, OnboardingRequestEvaluatorService onboardingRequestEvaluatorService, Onboarding2EvaluationMapper onboarding2EvaluationMapper, ErrorNotifierService errorNotifierService, ObjectMapper objectMapper) {
        this.onboardingCheckService = onboardingCheckService;
        this.authoritiesDataRetrieverService = authoritiesDataRetrieverService;
        this.onboardingRequestEvaluatorService = onboardingRequestEvaluatorService;
        this.onboarding2EvaluationMapper = onboarding2EvaluationMapper;
        this.errorNotifierService = errorNotifierService;

        this.objectReader = objectMapper.readerFor(OnboardingDTO.class);
    }


    /**
     * This component will take a {@link OnboardingDTO} and will calculate the {@link EvaluationDTO}
     */
    @Override
    public Flux<EvaluationDTO> execute(Flux<Message<String>> messageFlux) {
        return messageFlux.flatMap(this::execute);
    }

    private Mono<EvaluationDTO> execute(Message<String> message) {
        try {
            Map<String, Object> onboardingContext = new HashMap<>();

            log.info("[ONBOARDING_REQUEST] Evaluating onboarding request {}", message.getPayload());

            OnboardingDTO onboardingRequest = deserializeMessage(message);

            if(onboardingRequest!=null) {
                EvaluationDTO rejectedRequest = evaluateOnboardingChecks(onboardingRequest, onboardingContext);
                if (rejectedRequest != null) {
                    return Mono.just(rejectedRequest);
                } else {
                    log.debug("[ONBOARDING_REQUEST] [ONBOARDING_CHECK] onboarding of user {} into initiative {} resulted into successful preliminary checks", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());
                    return retrieveAuthoritiesDataAndEvaluateRequest(onboardingRequest, onboardingContext, message)

                            .onErrorResume(e -> {
                                // TODO we should persist it as ONBOARDING_KO instead?
                                errorNotifierService.notifyAdmissibility(message, "An error occurred handling onboarding request", true, e);
                                return Mono.empty();
                            });
                }
            } else {
                return Mono.empty();
            }
        } catch (RuntimeException e){
            // TODO we should persist it as ONBOARDING_KO instead?
            errorNotifierService.notifyAdmissibility(message, "Unexpected error", true, e);
            return Mono.empty();
        }
    }

    private OnboardingDTO deserializeMessage(Message<String> message) {
        return Utils.deserializeMessage(message, objectReader, e -> errorNotifierService.notifyAdmissibility(message, "Unexpected JSON", true, e));
    }

    private EvaluationDTO evaluateOnboardingChecks(OnboardingDTO onboardingRequest, Map<String, Object> onboardingContext) {
        OnboardingRejectionReason rejectionReason = onboardingCheckService.check(onboardingRequest, onboardingContext);
        if (rejectionReason != null) {
            log.info("[ONBOARDING_REQUEST] [ONBOARDING_KO] [ONBOARDING_CHECK] Onboarding request failed: {}",rejectionReason);
            return onboarding2EvaluationMapper.apply(onboardingRequest, readInitiativeConfigFromContext(onboardingContext), Collections.singletonList(rejectionReason));
        } else return null;
    }

    private Mono<EvaluationDTO> retrieveAuthoritiesDataAndEvaluateRequest(OnboardingDTO onboardingRequest, Map<String, Object> onboardingContext, Message<String> message) {
        final InitiativeConfig initiativeConfig = readInitiativeConfigFromContext(onboardingContext);

        return authoritiesDataRetrieverService.retrieve(onboardingRequest, initiativeConfig, message)
                .flatMap(r -> onboardingRequestEvaluatorService.evaluate(r, initiativeConfig));
    }

    private InitiativeConfig readInitiativeConfigFromContext(Map<String, Object> onboardingContext) {
        return (InitiativeConfig) onboardingContext.get(ONBOARDING_CONTEXT_INITIATIVE_KEY);
    }

}
