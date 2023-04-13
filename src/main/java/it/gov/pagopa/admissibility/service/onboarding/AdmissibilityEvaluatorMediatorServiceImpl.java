package it.gov.pagopa.admissibility.service.onboarding;

import com.azure.spring.messaging.AzureHeaders;
import com.azure.spring.messaging.checkpoint.Checkpointer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.exception.OnboardingException;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.ErrorNotifierService;
import it.gov.pagopa.admissibility.utils.PerformanceLogger;
import it.gov.pagopa.admissibility.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static it.gov.pagopa.admissibility.utils.OnboardingConstants.ONBOARDING_CONTEXT_INITIATIVE_KEY;

@Service
@Slf4j
public class AdmissibilityEvaluatorMediatorServiceImpl implements AdmissibilityEvaluatorMediatorService {

    private final OnboardingContextHolderService onboardingContextHolderService;
    private final OnboardingCheckService onboardingCheckService;
    private final AuthoritiesDataRetrieverService authoritiesDataRetrieverService;
    private final OnboardingRequestEvaluatorService onboardingRequestEvaluatorService;
    private final Onboarding2EvaluationMapper onboarding2EvaluationMapper;
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    private final OnboardingNotifierService onboardingNotifierService;
    private final RankingNotifierService rankingNotifierService;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    public AdmissibilityEvaluatorMediatorServiceImpl(
            OnboardingContextHolderService onboardingContextHolderService,
            OnboardingCheckService onboardingCheckService,
            AuthoritiesDataRetrieverService authoritiesDataRetrieverService,
            OnboardingRequestEvaluatorService onboardingRequestEvaluatorService,
            Onboarding2EvaluationMapper onboarding2EvaluationMapper,
            ErrorNotifierService errorNotifierService,
            ObjectMapper objectMapper,
            OnboardingNotifierService onboardingNotifierService,
            RankingNotifierService rankingNotifierService) {
        this.onboardingContextHolderService = onboardingContextHolderService;
        this.onboardingCheckService = onboardingCheckService;
        this.authoritiesDataRetrieverService = authoritiesDataRetrieverService;
        this.onboardingRequestEvaluatorService = onboardingRequestEvaluatorService;
        this.onboarding2EvaluationMapper = onboarding2EvaluationMapper;
        this.errorNotifierService = errorNotifierService;

        this.objectReader = objectMapper.readerFor(OnboardingDTO.class);
        this.onboardingNotifierService = onboardingNotifierService;
        this.rankingNotifierService = rankingNotifierService;
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
        long startTime = System.currentTimeMillis();

        return Mono.just(message)
                .flatMap(this::execute)
                .doOnNext(evaluationDTO -> {
                    if (evaluationDTO instanceof EvaluationCompletedDTO evaluation) {
                        callOnboardingNotifier(evaluation);
                        if (evaluation.getRankingValue() != null) {
                            callRankingNotifier(onboarding2EvaluationMapper.apply(evaluation));
                        }
                    } else {
                        callRankingNotifier((RankingRequestDTO) evaluationDTO);
                    }
                })
                .onErrorResume(e -> {
                    // TODO we should send it as ONBOARDING_KO (instead or rescheduling)?
                    errorNotifierService.notifyAdmissibility(message, "[ADMISSIBILITY_ONBOARDING_REQUEST] An error occurred handling onboarding request", true, e);
                    return Mono.empty();
                })
                .doFinally(o -> {
                    Checkpointer checkpointer = message.getHeaders().get(AzureHeaders.CHECKPOINTER, Checkpointer.class);
                    if (checkpointer != null) {
                        checkpointer.success()
                                .doOnSuccess(success -> log.debug("Successfully checkpoint {}", message.getPayload()))
                                .doOnError(e -> log.error("Fail to checkpoint the message", e))
                                .subscribe();
                    }
                   PerformanceLogger.logTiming("ONBOARDING_REQUEST", startTime, message.getPayload());
                });
    }

    private Mono<EvaluationDTO> execute(Message<String> message) {

        log.info("[ONBOARDING_REQUEST] Evaluating onboarding request {}", Utils.readMessagePayload(message));

        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .flatMap(onboardingRequest ->
                        onboardingContextHolderService.getInitiativeConfig(onboardingRequest.getInitiativeId())
                                .map(Optional::of)
                                .switchIfEmpty(Mono.just(Optional.empty()))

                                .flatMap(initiativeConfig -> execute(message, onboardingRequest, initiativeConfig.orElse(null)))
                );
    }

    private Mono<EvaluationDTO> execute(Message<String> message, OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig) {
        Map<String, Object> onboardingContext = new HashMap<>();
        onboardingContext.put(ONBOARDING_CONTEXT_INITIATIVE_KEY, initiativeConfig);
        if (onboardingRequest != null) {
            EvaluationDTO rejectedRequest = evaluateOnboardingChecks(onboardingRequest, initiativeConfig, onboardingContext);
            if (rejectedRequest != null) {
                return Mono.just(rejectedRequest);
            } else {
                log.debug("[ONBOARDING_REQUEST] [ONBOARDING_CHECK] onboarding of user {} into initiative {} resulted into successful preliminary checks", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());
                return retrieveAuthoritiesDataAndEvaluateRequest(onboardingRequest, initiativeConfig, message);
            }
        } else {
            return Mono.empty();
        }
    }

    private OnboardingDTO deserializeMessage(Message<String> message) {
        return Utils.deserializeMessage(message, objectReader, e -> errorNotifierService.notifyAdmissibility(message, "[ADMISSIBILITY_ONBOARDING_REQUEST] Unexpected JSON", true, e));
    }

    private EvaluationDTO evaluateOnboardingChecks(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Map<String, Object> onboardingContext) {
        OnboardingRejectionReason rejectionReason = onboardingCheckService.check(onboardingRequest, initiativeConfig, onboardingContext);
        if (rejectionReason != null) {
            log.info("[ONBOARDING_REQUEST] [ONBOARDING_KO] [ONBOARDING_CHECK] Onboarding request failed: {}", rejectionReason);
            return onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, Collections.singletonList(rejectionReason));
        } else return null;
    }

    private Mono<EvaluationDTO> retrieveAuthoritiesDataAndEvaluateRequest(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message) {
        return authoritiesDataRetrieverService.retrieve(onboardingRequest, initiativeConfig, message)
                .flatMap(r -> onboardingRequestEvaluatorService.evaluate(r, initiativeConfig))
                .onErrorResume(OnboardingException.class, e -> {
                    log.info(e.getMessage());
                    return Mono.just(onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, e.getRejectionReasons()));
                });
    }

    private void callOnboardingNotifier(EvaluationCompletedDTO evaluationCompletedDTO) {
        log.info("[ONBOARDING_REQUEST] notifying onboarding request to outcome topic: {}", evaluationCompletedDTO);
        try {
            if (!onboardingNotifierService.notify(evaluationCompletedDTO)) {
                throw new IllegalStateException("[ADMISSIBILITY_ONBOARDING_REQUEST] Something gone wrong while onboarding notify");
            }
        } catch (Exception e) {
            log.error("[UNEXPECTED_ONBOARDING_PROCESSOR_ERROR] Unexpected error occurred publishing onboarding result: {}", evaluationCompletedDTO);
            errorNotifierService.notifyAdmissibilityOutcome(OnboardingNotifierServiceImpl.buildMessage(evaluationCompletedDTO), "[ADMISSIBILITY] An error occurred while publishing the onboarding evaluation result", true, e);
        }
    }

    private void callRankingNotifier(RankingRequestDTO rankingRequestDTO) {
        log.info("[ONBOARDING_REQUEST] notifying onboarding request to ranking topic: {}", rankingRequestDTO);
        try {
            if (!rankingNotifierService.notify(rankingRequestDTO)) {
                throw new IllegalStateException("[ADMISSIBILITY_ONBOARDING_REQUEST] Something gone wrong while ranking notify");
            }
        } catch (Exception e) {
            log.error("[UNEXPECTED_ONBOARDING_PROCESSOR_ERROR] Unexpected error occurred publishing onboarding result: {}", rankingRequestDTO);
            errorNotifierService.notifyRankingRequest(RankingNotifierServiceImpl.buildMessage(rankingRequestDTO), "[ADMISSIBILITY] An error occurred while publishing the ranking request", true, e);
        }
    }

}
