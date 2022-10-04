package it.gov.pagopa.admissibility.service;


import com.azure.spring.messaging.AzureHeaders;
import com.azure.spring.messaging.checkpoint.Checkpointer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.AuthoritiesDataRetrieverService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingCheckService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingNotifierService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingRequestEvaluatorService;
import it.gov.pagopa.admissibility.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
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

    private final OnboardingNotifierService onboardingNotifierService;

    public AdmissibilityEvaluatorMediatorServiceImpl(OnboardingCheckService onboardingCheckService, AuthoritiesDataRetrieverService authoritiesDataRetrieverService, OnboardingRequestEvaluatorService onboardingRequestEvaluatorService, Onboarding2EvaluationMapper onboarding2EvaluationMapper, ErrorNotifierService errorNotifierService, ObjectMapper objectMapper, OnboardingNotifierService onboardingNotifierService) {
        this.onboardingCheckService = onboardingCheckService;
        this.authoritiesDataRetrieverService = authoritiesDataRetrieverService;
        this.onboardingRequestEvaluatorService = onboardingRequestEvaluatorService;
        this.onboarding2EvaluationMapper = onboarding2EvaluationMapper;
        this.errorNotifierService = errorNotifierService;

        this.objectReader = objectMapper.readerFor(OnboardingDTO.class);
        this.onboardingNotifierService = onboardingNotifierService;
    }

    /**
     * This component will take a {@link OnboardingDTO} and will calculate the {@link EvaluationDTO}
     */
    @Override
    public void execute(Flux<Message<String>> messageFlux) {
        messageFlux
                .flatMap(this::executeAndCommit)
                .subscribe(evaluationDTO -> log.info("[[ADMISSIBILITY_ONBOARDING_REQUEST]] Processed offsets committed successfully"));
    }

    private Mono<EvaluationDTO> executeAndCommit(Message<String> message) {
        return Mono.just(message)
                .flatMap(this::execute)
                .doOnNext(evaluationDTO -> {
                    Exception exception=null;
                    try {
                        if (!onboardingNotifierService.notify(evaluationDTO)) {
                            exception = new IllegalStateException("[ADMISSIBILITY_ONBOARDING_REQUEST] Something gone wrong while transaction notify");
                        }
                    } catch (Exception e){
                        exception = e;
                    }
                    if (exception != null) {
                        log.error("[UNEXPECTED_ONBOARDING_PROCESSOR_ERROR] Unexpected error occurred publishing onboarding result: {}", evaluationDTO);
                        errorNotifierService.notifyAdmissibility(new GenericMessage<>(evaluationDTO, message.getHeaders()), "[ADMISSIBILITY] An error occurred while publishing the onboarding evaluation result", true, exception);
                    }
                })
                .onErrorResume(e -> {
                    // TODO we should persist it as ONBOARDING_KO instead?
                    errorNotifierService.notifyAdmissibility(message, "[ADMISSIBILITY_ONBOARDING_REQUEST] An error occurred handling onboarding request", true, e);
                    return Mono.empty();
                })
                .doFinally(o-> {
                    // commit subscribe e log successo (debug) o error (ERROR)
                    Checkpointer checkpointer = message.getHeaders().get(AzureHeaders.CHECKPOINTER, Checkpointer.class);
                    if (checkpointer != null) {
                        checkpointer.success()
                                .doOnSuccess(success -> log.debug("Successfully checkpoint {}", message.getPayload()))
                                .doOnError(e -> log.error("Fail to checkpoint the message", e))
                                .subscribe();
                    }
                });
    }

    private Mono<EvaluationDTO> execute(Message<String> message) {
        Map<String, Object> onboardingContext = new HashMap<>();

        log.info("[ONBOARDING_REQUEST] Evaluating onboarding request {}", message.getPayload());

        OnboardingDTO onboardingRequest = deserializeMessage(message);

        if(onboardingRequest!=null) {
            EvaluationDTO rejectedRequest = evaluateOnboardingChecks(onboardingRequest, onboardingContext);
            if (rejectedRequest != null) {
                return Mono.just(rejectedRequest);
            } else {
                log.debug("[ONBOARDING_REQUEST] [ONBOARDING_CHECK] onboarding of user {} into initiative {} resulted into successful preliminary checks", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());
                return retrieveAuthoritiesDataAndEvaluateRequest(onboardingRequest, onboardingContext, message);
            }
        } else {
            return Mono.empty();
        }
    }

    private OnboardingDTO deserializeMessage(Message<String> message) {
        return Utils.deserializeMessage(message, objectReader, e -> errorNotifierService.notifyAdmissibility(message, "[ADMISSIBILITY_ONBOARDING_REQUEST] Unexpected JSON", true, e));
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
