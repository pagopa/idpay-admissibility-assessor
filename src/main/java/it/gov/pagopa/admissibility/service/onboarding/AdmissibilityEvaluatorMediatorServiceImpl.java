package it.gov.pagopa.admissibility.service.onboarding;

import com.azure.spring.messaging.AzureHeaders;
import com.azure.spring.messaging.checkpoint.Checkpointer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.exception.OnboardingException;
import it.gov.pagopa.admissibility.exception.SkipAlreadyRankingFamilyOnBoardingException;
import it.gov.pagopa.admissibility.exception.WaitingFamilyOnBoardingException;
import it.gov.pagopa.admissibility.mapper.Onboarding2EvaluationMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.AdmissibilityErrorNotifierService;
import it.gov.pagopa.admissibility.service.onboarding.evaluate.OnboardingRequestEvaluatorService;
import it.gov.pagopa.admissibility.service.onboarding.family.OnboardingFamilyEvaluationService;
import it.gov.pagopa.admissibility.service.onboarding.notifier.OnboardingNotifierService;
import it.gov.pagopa.admissibility.service.onboarding.notifier.OnboardingNotifierServiceImpl;
import it.gov.pagopa.admissibility.service.onboarding.notifier.RankingNotifierService;
import it.gov.pagopa.admissibility.service.onboarding.notifier.RankingNotifierServiceImpl;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import it.gov.pagopa.common.utils.CommonUtilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static it.gov.pagopa.admissibility.utils.OnboardingConstants.ONBOARDING_CONTEXT_INITIATIVE_KEY;

@Service
@Slf4j
public class AdmissibilityEvaluatorMediatorServiceImpl implements AdmissibilityEvaluatorMediatorService {
    private static final List<String> REJECTION_REASON_CHECK_DATE_FAIL = List.of(OnboardingConstants.REJECTION_REASON_TC_CONSENSUS_DATETIME_FAIL, OnboardingConstants.REJECTION_REASON_CRITERIA_CONSENSUS_DATETIME_FAIL);

    private final int maxOnboardingRequestRetry;

    private final OnboardingContextHolderService onboardingContextHolderService;
    private final OnboardingCheckService onboardingCheckService;
    private final OnboardingFamilyEvaluationService onboardingFamilyEvaluationService;
    private final AuthoritiesDataRetrieverService authoritiesDataRetrieverService;
    private final OnboardingRequestEvaluatorService onboardingRequestEvaluatorService;
    private final Onboarding2EvaluationMapper onboarding2EvaluationMapper;
    private final AdmissibilityErrorNotifierService admissibilityErrorNotifierService;

    private final ObjectReader objectReader;

    private final OnboardingNotifierService onboardingNotifierService;
    private final RankingNotifierService rankingNotifierService;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    public AdmissibilityEvaluatorMediatorServiceImpl(
            @Value("${app.onboarding-request.max-retry}") int maxOnboardingRequestRetry,

            OnboardingContextHolderService onboardingContextHolderService,
            OnboardingCheckService onboardingCheckService,
            OnboardingFamilyEvaluationService onboardingFamilyEvaluationService, AuthoritiesDataRetrieverService authoritiesDataRetrieverService,
            OnboardingRequestEvaluatorService onboardingRequestEvaluatorService,
            Onboarding2EvaluationMapper onboarding2EvaluationMapper,
            AdmissibilityErrorNotifierService admissibilityErrorNotifierService,
            ObjectMapper objectMapper,
            OnboardingNotifierService onboardingNotifierService,
            RankingNotifierService rankingNotifierService) {
        this.maxOnboardingRequestRetry = maxOnboardingRequestRetry;
        this.onboardingContextHolderService = onboardingContextHolderService;
        this.onboardingCheckService = onboardingCheckService;
        this.onboardingFamilyEvaluationService = onboardingFamilyEvaluationService;
        this.authoritiesDataRetrieverService = authoritiesDataRetrieverService;
        this.onboardingRequestEvaluatorService = onboardingRequestEvaluatorService;
        this.onboarding2EvaluationMapper = onboarding2EvaluationMapper;
        this.admissibilityErrorNotifierService = admissibilityErrorNotifierService;

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
                .subscribe(evaluationDTO -> log.info("[ADMISSIBILITY_ONBOARDING_REQUEST] Processed offsets committed successfully"));
    }

    private Mono<EvaluationDTO> executeAndCommit(Message<String> message) {
        long startTime = System.currentTimeMillis();

        return Mono.just(message)
                .flatMap(this::execute)
                .map(req2ev -> {
                    OnboardingDTO request = req2ev.getKey();
                    EvaluationDTO evaluationDTO = req2ev.getValue();

                    if (evaluationDTO instanceof EvaluationCompletedDTO evaluation) {
                        callOnboardingNotifier(evaluation,request);
                        if (evaluation.getRankingValue() != null) {
                            callRankingNotifier(onboarding2EvaluationMapper.apply(request, evaluation));
                        }
                        inviteFamilyMembers(request, evaluation);
                    } else {
                        callRankingNotifier((RankingRequestDTO) evaluationDTO);
                    }

                    return evaluationDTO;
                })
                .onErrorResume(e -> {
                    admissibilityErrorNotifierService.notifyAdmissibility(message, "[ADMISSIBILITY_ONBOARDING_REQUEST] An error occurred handling onboarding request", true, e);
                    return Mono.empty();
                })

                .publishOn(Schedulers.boundedElastic())
                .switchIfEmpty(commitMessage(startTime, message, Mono.empty()))
                .flatMap(o -> commitMessage(startTime, message, Mono.just(o)));
    }

    private Mono<EvaluationDTO> commitMessage(long startTime, Message<String> message, Mono<EvaluationDTO> then) {
        return Mono.defer(() -> {
            Checkpointer checkpointer = message.getHeaders().get(AzureHeaders.CHECKPOINTER, Checkpointer.class);
            Mono<EvaluationDTO> mono;
            if (checkpointer != null) {
                mono = checkpointer.success()
                        .doOnSuccess(success -> log.debug("Successfully checkpoint {}", CommonUtilities.readMessagePayload(message)))
                        .doOnError(e -> log.error("Fail to checkpoint the message", e))
                        .then(then);
            } else {
                mono = then;
            }
            return PerformanceLogger.logTimingFinally("ONBOARDING_REQUEST", startTime, mono, CommonUtilities.readMessagePayload(message));
        });
    }

    private Mono<Pair<OnboardingDTO, EvaluationDTO>> execute(Message<String> message) {

        log.info("[ONBOARDING_REQUEST] Evaluating onboarding request {}", CommonUtilities.readMessagePayload(message));

        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .flatMap(onboardingRequest ->
                        onboardingContextHolderService.getInitiativeConfig(onboardingRequest.getInitiativeId())
                                .map(Optional::of)
                                .switchIfEmpty(Mono.just(Optional.empty()))

                                .flatMap(initiativeConfig -> execute(message, onboardingRequest, initiativeConfig.orElse(null)))
                                .map(ev -> Pair.of(onboardingRequest, ev))
                );
    }

    private Mono<EvaluationDTO> execute(Message<String> message, OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig) {
        Map<String, Object> onboardingContext = new HashMap<>();
        onboardingContext.put(ONBOARDING_CONTEXT_INITIATIVE_KEY, initiativeConfig);
        if (onboardingRequest != null) {
            EvaluationDTO rejectedRequest = evaluateOnboardingChecks(onboardingRequest, initiativeConfig, onboardingContext);
            if (rejectedRequest != null) {
                return checkRejectionType(message, onboardingRequest, initiativeConfig, rejectedRequest);
            } else {
                log.debug("[ONBOARDING_REQUEST] [ONBOARDING_CHECK] onboarding of user {} into initiative {} resulted into successful preliminary checks", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());
                return checkOnboardingFamily(onboardingRequest, initiativeConfig, message, true)
                        .switchIfEmpty(retrieveAuthoritiesDataAndEvaluateRequest(onboardingRequest, initiativeConfig, message))

                        .onErrorResume(WaitingFamilyOnBoardingException.class, e -> Mono.empty())

                        .onErrorResume(SkipAlreadyRankingFamilyOnBoardingException.class, e -> Mono.empty())

                        .onErrorResume(e -> {
                            log.error("[ONBOARDING_REQUEST] Something gone wrong while handling onboarding request{} of userId {} into initiativeId {}",
                                    onboardingRequest.isBudgetReserved() ? " (BUDGET_RESERVED)" : "",
                                    onboardingRequest.getUserId(), onboardingRequest.getInitiativeId(), e);

                            String retryHeaderValue = readRetryHeader(message);

                            if (retryHeaderValue == null || Integer.parseInt(retryHeaderValue) < maxOnboardingRequestRetry) {
                                log.info("[ONBOARDING_REQUEST] letting the error-topic-handler to resubmit the request");
                                return Mono.error(e);
                            } else {
                                return Mono.just(onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig
                                        , List.of(new OnboardingRejectionReason(
                                                OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR,
                                                OnboardingConstants.REJECTION_REASON_GENERIC_ERROR,
                                                null, null, null
                                        ))));
                            }
                        });
            }
        } else {
            return Mono.empty();
        }
    }

    @NotNull
    private Mono<EvaluationDTO> checkRejectionType(Message<String> message, OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, EvaluationDTO rejectedRequest) {
        if(rejectedRequest instanceof EvaluationCompletedDTO completedDTO
                && initiativeConfig !=null
                && InitiativeGeneralDTO.BeneficiaryTypeEnum.NF.equals(initiativeConfig.getBeneficiaryType())
                && completedDTO.getOnboardingRejectionReasons().stream().anyMatch(o -> REJECTION_REASON_CHECK_DATE_FAIL.contains(o.getCode()))){
            return checkOnboardingFamily(onboardingRequest, initiativeConfig, message, false)
                    .switchIfEmpty(Mono.just(rejectedRequest))
                    .onErrorResume(WaitingFamilyOnBoardingException.class, e -> Mono.empty())
                    .onErrorResume(SkipAlreadyRankingFamilyOnBoardingException.class, e -> Mono.empty());
        }
        return Mono.just(rejectedRequest);
    }

    private static String readRetryHeader(Message<String> message) {
        Object retryHeader = message.getHeaders().get(KafkaConstants.ERROR_MSG_HEADER_RETRY);

        String retryHeaderValue;
        if(retryHeader instanceof String retryString){ // ServiceBus return it as String
            retryHeaderValue = retryString;
        } else if(retryHeader instanceof byte[] retryBytes) { // Kafka return it as byte[]
            retryHeaderValue = new String(retryBytes, StandardCharsets.UTF_8);
        } else {
            retryHeaderValue = null;
        }
        return retryHeaderValue;
    }

    private OnboardingDTO deserializeMessage(Message<String> message) {
        return CommonUtilities.deserializeMessage(message, objectReader, e -> admissibilityErrorNotifierService.notifyAdmissibility(message, "[ADMISSIBILITY_ONBOARDING_REQUEST] Unexpected JSON", true, e));
    }

    private EvaluationDTO evaluateOnboardingChecks(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Map<String, Object> onboardingContext) {
        OnboardingRejectionReason rejectionReason = onboardingCheckService.check(onboardingRequest, initiativeConfig, onboardingContext);
        if (rejectionReason != null) {
            log.info("[ONBOARDING_REQUEST] [ONBOARDING_KO] [ONBOARDING_CHECK] Onboarding request failed: {}", rejectionReason);
            return onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, Collections.singletonList(rejectionReason));
        } else return null;
    }

    private Mono<EvaluationDTO> checkOnboardingFamily(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message, boolean retrieveFamily) {
        if(isFamilyInitiative(initiativeConfig)){
            return onboardingFamilyEvaluationService.checkOnboardingFamily(onboardingRequest, initiativeConfig, message, retrieveFamily);
        } else {
            return Mono.empty();
        }
    }

    private static boolean isFamilyInitiative(InitiativeConfig initiativeConfig) {
        return InitiativeGeneralDTO.BeneficiaryTypeEnum.NF.equals(initiativeConfig.getBeneficiaryType());
    }

    private Mono<EvaluationDTO> retrieveAuthoritiesDataAndEvaluateRequest(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message) {
        return authoritiesDataRetrieverService.retrieve(onboardingRequest, initiativeConfig, message)
                .flatMap(r -> onboardingRequestEvaluatorService.evaluate(r, initiativeConfig))
                .onErrorResume(OnboardingException.class, e -> {
                    log.info(e.getMessage());
                    return Mono.just(onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, e.getRejectionReasons()));
                })

                .flatMap(ev -> {
                    if(isFamilyInitiative(initiativeConfig)){
                        return onboardingFamilyEvaluationService.updateOnboardingFamilyOutcome(onboardingRequest.getFamily(), initiativeConfig, ev);
                    } else {
                        return Mono.just(ev);
                    }
                });
    }

    private void callOnboardingNotifier(EvaluationCompletedDTO evaluationCompletedDTO, OnboardingDTO onboardingDTO) {
        log.info("[ONBOARDING_REQUEST] notifying onboarding request to outcome topic: {}", evaluationCompletedDTO);
        try {
            evaluationCompletedDTO.setServiceId(onboardingDTO.getServiceId());
            if (!onboardingNotifierService.notify(evaluationCompletedDTO)) {
                throw new IllegalStateException("[ADMISSIBILITY_ONBOARDING_REQUEST] Something gone wrong while onboarding notify");
            }
        } catch (Exception e) {
            log.error("[UNEXPECTED_ONBOARDING_PROCESSOR_ERROR] Unexpected error occurred publishing onboarding result: {}", evaluationCompletedDTO);
            admissibilityErrorNotifierService.notifyAdmissibilityOutcome(OnboardingNotifierServiceImpl.buildMessage(evaluationCompletedDTO), "[ONBOARDING_REQUEST] An error occurred while publishing the onboarding evaluation result", true, e);
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
            admissibilityErrorNotifierService.notifyRankingRequest(RankingNotifierServiceImpl.buildMessage(rankingRequestDTO), "[ONBOARDING_REQUEST] An error occurred while publishing the ranking request", true, e);
        }
    }

    private void inviteFamilyMembers(OnboardingDTO request, EvaluationCompletedDTO evaluation) {
        if(request.getFamily()!=null){
            if(OnboardingEvaluationStatus.ONBOARDING_OK.equals(evaluation.getStatus())){
                callFamilyMembersNotifier(request, evaluation, OnboardingEvaluationStatus.DEMANDED);
            } else if (OnboardingEvaluationStatus.ONBOARDING_KO.equals(evaluation.getStatus())){
                log.info("[FAMILY_MEMBERS_NOTIFY_KO] Notify onboarding KO to member of the family {}", request.getFamily().getFamilyId());
                evaluation.getOnboardingRejectionReasons()
                        .add(OnboardingRejectionReason.builder()
                                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.FAMILY_CRITERIA_KO)
                                .code(OnboardingConstants.REJECTION_REASON_FAMILY_CRITERIA_FAIL)
                                .detail("Nucleo familiare non soddisfa i requisiti")
                                .build());

                callFamilyMembersNotifier(request, evaluation, OnboardingEvaluationStatus.ONBOARDING_KO);
            }
        }

    }

    private void callFamilyMembersNotifier(OnboardingDTO request, EvaluationCompletedDTO evaluation, OnboardingEvaluationStatus status) {
        request.getFamily().getMemberIds().forEach(userId -> {
            if(!userId.equals(request.getUserId())){
                callOnboardingNotifier(evaluation.toBuilder()
                        .userId(userId)
                        .status(status)
                        .build(),
                        request);
            }
        });
    }

}
