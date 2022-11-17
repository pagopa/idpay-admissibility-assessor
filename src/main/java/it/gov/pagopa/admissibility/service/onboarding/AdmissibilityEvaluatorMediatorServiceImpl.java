package it.gov.pagopa.admissibility.service.onboarding;

import com.azure.spring.messaging.AzureHeaders;
import com.azure.spring.messaging.checkpoint.Checkpointer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.RankingRequestDTO;
import it.gov.pagopa.admissibility.mapper.Evaluation2RankingRequestMapper;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.ErrorNotifierService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.springframework.data.util.Pair;
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
    private final Evaluation2RankingRequestMapper evaluation2RankingRequestMapper;
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    private final OnboardingNotifierService onboardingNotifierService;
    private final RankingNotifierService rankingNotifierService;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    public AdmissibilityEvaluatorMediatorServiceImpl(OnboardingCheckService onboardingCheckService, AuthoritiesDataRetrieverService authoritiesDataRetrieverService, OnboardingRequestEvaluatorService onboardingRequestEvaluatorService, Onboarding2EvaluationMapper onboarding2EvaluationMapper, Evaluation2RankingRequestMapper evaluation2RankingRequestMapper, ErrorNotifierService errorNotifierService, ObjectMapper objectMapper, OnboardingNotifierService onboardingNotifierService, RankingNotifierService rankingNotifierService) {
        this.onboardingCheckService = onboardingCheckService;
        this.authoritiesDataRetrieverService = authoritiesDataRetrieverService;
        this.onboardingRequestEvaluatorService = onboardingRequestEvaluatorService;
        this.onboarding2EvaluationMapper = onboarding2EvaluationMapper;
        this.evaluation2RankingRequestMapper = evaluation2RankingRequestMapper;
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

    private Mono<ImmutableTriple<EvaluationDTO, InitiativeConfig,OnboardingDTO>> executeAndCommit(Message<String> message) {
        long startTime = System.currentTimeMillis();

        return Mono.just(message)
                .flatMap(this::execute)
                .doOnNext(triple -> {
                    if(triple.left.getStatus().equals(OnboardingConstants.ONBOARDING_STATUS_KO)
                            || !triple.middle.getRankingInitiative().equals(Boolean.TRUE)) {
                        callOnboardingNotifier(triple);
                    } else {
                        callRankingNotifier(triple);
                    }})
                .onErrorResume(e -> {
                    // TODO we should send it as ONBOARDING_KO (instead or rescheduling)?
                    errorNotifierService.notifyAdmissibility(message, "[ADMISSIBILITY_ONBOARDING_REQUEST] An error occurred handling onboarding request", true, e);
                    return Mono.empty();
                })
                .doFinally(o-> {
                    Checkpointer checkpointer = message.getHeaders().get(AzureHeaders.CHECKPOINTER, Checkpointer.class);
                    if (checkpointer != null) {
                        checkpointer.success()
                                .doOnSuccess(success -> log.debug("Successfully checkpoint {}", message.getPayload()))
                                .doOnError(e -> log.error("Fail to checkpoint the message", e))
                                .subscribe();
                    }
                    log.info("[PERFORMANCE_LOG] [ONBOARDING_REQUEST] Time occurred to perform business logic: {} ms {}", System.currentTimeMillis() - startTime, message.getPayload());
                });
    }

    private Mono<ImmutableTriple<EvaluationDTO, InitiativeConfig,OnboardingDTO>> execute(Message<String> message) { //TODO
        Map<String, Object> onboardingContext = new HashMap<>();

        log.info("[ONBOARDING_REQUEST] Evaluating onboarding request {}", message.getPayload());

        OnboardingDTO onboardingRequest = deserializeMessage(message);

        if(onboardingRequest!=null) {
            EvaluationDTO rejectedRequest = evaluateOnboardingChecks(onboardingRequest, onboardingContext);
            if (rejectedRequest != null) {
                return Mono.just(ImmutableTriple.of(rejectedRequest,null,onboardingRequest)); //TODO check response
            } else {
                log.debug("[ONBOARDING_REQUEST] [ONBOARDING_CHECK] onboarding of user {} into initiative {} resulted into successful preliminary checks", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());
                return retrieveAuthoritiesDataAndEvaluateRequest(onboardingRequest, onboardingContext, message)
                        .flatMap(r -> Mono.just(ImmutableTriple.of(r.getFirst(),r.getSecond(),onboardingRequest)));
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

    private Mono<Pair<EvaluationDTO, InitiativeConfig>> retrieveAuthoritiesDataAndEvaluateRequest(OnboardingDTO onboardingRequest, Map<String, Object> onboardingContext, Message<String> message) {
        final InitiativeConfig initiativeConfig = readInitiativeConfigFromContext(onboardingContext);

        return authoritiesDataRetrieverService.retrieve(onboardingRequest, initiativeConfig, message)
                .flatMap(r -> onboardingRequestEvaluatorService.evaluate(r, initiativeConfig))
                .flatMap(n -> Mono.just(Pair.of(n, initiativeConfig)));
    }

    private InitiativeConfig readInitiativeConfigFromContext(Map<String, Object> onboardingContext) {
        return (InitiativeConfig) onboardingContext.get(ONBOARDING_CONTEXT_INITIATIVE_KEY);
    }

    private void callOnboardingNotifier(ImmutableTriple<EvaluationDTO, InitiativeConfig, OnboardingDTO> triple) {
        try {
            if (!onboardingNotifierService.notify(triple.left)) {
                throw new IllegalStateException("[ADMISSIBILITY_ONBOARDING_REQUEST] Something gone wrong while onboarding notify");
            }
        } catch (Exception e) {
            log.error("[UNEXPECTED_ONBOARDING_PROCESSOR_ERROR] Unexpected error occurred publishing onboarding result: {}", triple);
            errorNotifierService.notifyAdmissibilityOutcome(OnboardingNotifierServiceImpl.buildMessage(triple.left), "[ADMISSIBILITY] An error occurred while publishing the onboarding evaluation result", true, e);
        }
    }

    private void callRankingNotifier(ImmutableTriple<EvaluationDTO, InitiativeConfig, OnboardingDTO> triple) {
        RankingRequestDTO rankingRequestDTO = evaluation2RankingRequestMapper.apply(triple);try {
            if (!rankingNotifierService.notify(rankingRequestDTO)) {
                throw new IllegalStateException("[ADMISSIBILITY_ONBOARDING_REQUEST] Something gone wrong while ranking notify");
            }
        } catch (Exception e) {
            log.error("[UNEXPECTED_ONBOARDING_PROCESSOR_ERROR] Unexpected error occurred publishing onboarding result: {}", triple);
            errorNotifierService.notifyRankingRequest(RankingNotifierServiceImpl.buildMessage(rankingRequestDTO), "[ADMISSIBILITY] An error occurred while publishing the ranking request", true, e);
        }
    }

}
