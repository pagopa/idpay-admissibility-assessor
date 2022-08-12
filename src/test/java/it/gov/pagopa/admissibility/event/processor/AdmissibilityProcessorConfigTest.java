package it.gov.pagopa.admissibility.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.event.consumer.BeneficiaryRuleBuilderConsumerConfigIntegrationTest;
import it.gov.pagopa.admissibility.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.service.onboarding.AuthoritiesDataRetrieverService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingCheckService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.service.onboarding.RuleEngineService;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@TestPropertySource(properties = {
        "app.beneficiary-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.admissibility.service.build=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.onboarding=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.AdmissibilityEvaluatorMediatorServiceImpl=WARN",
})
class AdmissibilityProcessorConfigTest extends BaseIntegrationTest {
    public static final String EXHAUSTED_INITIATIVE_ID = "EXHAUSTED_INITIATIVE_ID";
    public static final String FAILING_BUDGET_RESERVATION_INITIATIVE_ID = "id_5_FAILING_BUDGET_RESERVATION";

    @SpyBean
    private OnboardingContextHolderService onboardingContextHolderServiceSpy;
    @SpyBean
    private OnboardingCheckService onboardingCheckServiceSpy;
    @SpyBean
    private AuthoritiesDataRetrieverService authoritiesDataRetrieverServiceSpy;
    @SpyBean
    private RuleEngineService ruleEngineServiceSpy;
    @SpyBean
    private InitiativeCountersRepository initiativeCountersRepositorySpy;

    private final int initiativesNumber = 5;

    @Test
    void testAdmissibilityOnboarding() throws JsonProcessingException {
        int validOnboardings = 1000; // use even number
        int notValidOnboarding = errorUseCases.size();
        long maxWaitingMs = 30000;

        publishOnboardingRules(validOnboardings);

        List<String> onboardings = new ArrayList<>(buildValidPayloads(errorUseCases.size(), validOnboardings / 2));
        onboardings.addAll(IntStream.range(0, notValidOnboarding).mapToObj(i -> errorUseCases.get(i).getFirst().get()).collect(Collectors.toList()));
        onboardings.addAll(buildValidPayloads(errorUseCases.size() + (validOnboardings / 2) + notValidOnboarding, validOnboardings / 2));

        long timePublishOnboardingStart = System.currentTimeMillis();
        onboardings.forEach(i -> publishIntoEmbeddedKafka(topicAdmissibilityProcessorRequest, null, null, i));
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeConsumerResponse = System.currentTimeMillis();
        List<ConsumerRecord<String, String>> payloadConsumed = consumeMessages(topicAdmissibilityProcessorOutcome, validOnboardings, maxWaitingMs);
        long timeEnd = System.currentTimeMillis();

        long timeConsumerResponseEnd = timeEnd - timeConsumerResponse;
        Assertions.assertEquals(validOnboardings, payloadConsumed.size());

        for (ConsumerRecord<String, String> p : payloadConsumed) {
            EvaluationDTO evaluation = objectMapper.readValue(p.value(), EvaluationDTO.class);
            checkResponse(evaluation);
        }

        checkErrorsPublished(notValidOnboarding, maxWaitingMs, errorUseCases);

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d) onboarding request messages: %d millis
                        Time spent to consume onboarding responses: %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                validOnboardings + notValidOnboarding,
                validOnboardings,
                notValidOnboarding,
                timePublishingOnboardingRequest,
                timeConsumerResponseEnd,
                timeEnd - timePublishOnboardingStart
        );
    }

    private void publishOnboardingRules(int onboardingsNumber) {
        int[] expectedRules = {0};
        IntStream.range(0, initiativesNumber + 2)
                .mapToObj(i -> {
                    final Initiative2BuildDTO initiative = Initiative2BuildDTOFaker.mockInstance(i);

                    BigDecimal budget;
                    if (initiative.getInitiativeId().endsWith("_" + initiativesNumber)) {
                        initiative.setInitiativeId(EXHAUSTED_INITIATIVE_ID);
                        budget = BigDecimal.ZERO;
                    } else if (initiative.getInitiativeId().endsWith("_" + (initiativesNumber+1))) {
                        initiative.setInitiativeId(FAILING_BUDGET_RESERVATION_INITIATIVE_ID);
                        budget = BigDecimal.TEN;
                    } else {
                        budget = initiative.getGeneral().getBeneficiaryBudget().multiply(BigDecimal.valueOf(onboardingsNumber));
                    }
                    initiative.getGeneral().setBudget(budget);

                    return initiative;
                })
                .peek(i -> expectedRules[0] += i.getBeneficiaryRule().getAutomatedCriteria().size())
                .forEach(i -> publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, null, null, i));


        BeneficiaryRuleBuilderConsumerConfigIntegrationTest.waitForKieContainerBuild(expectedRules[0], onboardingContextHolderServiceSpy);
    }

    private List<String> buildValidPayloads(int bias, int validOnboardings) {
        return IntStream.range(bias, bias + validOnboardings)
                .mapToObj(this::mockInstance)
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private OnboardingDTO mockInstance(int bias) {
        return useCases.get(bias % useCases.size()).getFirst().apply(bias);
    }

    private void checkResponse(EvaluationDTO evaluation) {
        String userId = evaluation.getUserId();
        int biasRetrieve = Integer.parseInt(userId.substring(7));
        useCases.get(biasRetrieve % useCases.size()).getSecond().accept(evaluation);
    }

    //region useCases
    private final List<Pair<Function<Integer, OnboardingDTO>, java.util.function.Consumer<EvaluationDTO>>> useCases = List.of(
            //successful case
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstance(bias, initiativesNumber),
                    evaluation -> {
                        Assertions.assertEquals(Collections.emptyList(), evaluation.getOnboardingRejectionReasons());
                        Assertions.assertEquals("ONBOARDING_OK", evaluation.getStatus());
                        assertEvaluationFields(evaluation, true);
                    }
            ),
            // TC consensus fail
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .tc(false)
                            .build(),
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED)
                                    .code(OnboardingConstants.REJECTION_REASON_CONSENSUS_TC_FAIL)
                                    .build(),
                            false)
            ),
            // PDND consensuns fail
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .pdndAccept(false)
                            .build(),
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED)
                                    .code(OnboardingConstants.REJECTION_REASON_CONSENSUS_PDND_FAIL)
                                    .build()
                            , false)
            ),
            // self declaration fail
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .selfDeclarationList(Map.of("DUMMY", false))
                            .build(),
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED)
                                    .code(OnboardingConstants.REJECTION_REASON_CONSENSUS_CHECK_SELF_DECLARATION_FAIL_FORMAT.formatted("DUMMY"))
                                    .build()
                            , false)
            ),
            // TC acceptance timestamp fail
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .tcAcceptTimestamp(LocalDateTime.now().withYear(1970))
                            .build(),
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                                    .code( OnboardingConstants.REJECTION_REASON_TC_CONSENSUS_DATETIME_FAIL)
                                    .build()
                           , true)
            ),
            // TC criteria acceptance timestamp fail
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .criteriaConsensusTimestamp(LocalDateTime.now().withYear(1970))
                            .build(),
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                                    .code(OnboardingConstants.REJECTION_REASON_CRITERIA_CONSENSUS_DATETIME_FAIL)
                                    .build()
                            , true)

            ),
            // AUTOMATED_CRITERIA fail
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .isee(BigDecimal.TEN)
                            .build(),
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL)
                                    .code(OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted("ISEE"))
                                    .authority("INPS")
                                    .authorityLabel("Istituto Nazionale Previdenza Sociale")
                                    .build()
                            , true)
            ),
            // exhausted initiative budget
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .initiativeId(EXHAUSTED_INITIATIVE_ID)
                            .build(),
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.BUDGET_EXHAUSTED)
                                    .code(OnboardingConstants.REJECTION_REASON_INITIATIVE_BUDGET_EXHAUSTED)
                                    .build()
                            , true)
            )
    );

    private void assertEvaluationFields(EvaluationDTO evaluation, boolean expectedInitiativeFieldFilled){
        Assertions.assertNotNull(evaluation.getUserId());
        Assertions.assertNotNull(evaluation.getInitiativeId());
        Assertions.assertNotNull(evaluation.getStatus());
        Assertions.assertNotNull(evaluation.getAdmissibilityCheckDate());
        Assertions.assertNotNull(evaluation.getOnboardingRejectionReasons());

        if(expectedInitiativeFieldFilled) {
            Assertions.assertNotNull(evaluation.getInitiativeName());
            Assertions.assertNotNull(evaluation.getOrganizationId());
        } else {
            Assertions.assertNull(evaluation.getInitiativeName());
            Assertions.assertNull(evaluation.getOrganizationId());
        }
    }

    private void checkKO(EvaluationDTO evaluation, OnboardingRejectionReason expectedRejectionReason, boolean expectedInitiativeFieldFilled) {
        Assertions.assertEquals("ONBOARDING_KO", evaluation.getStatus());
        Assertions.assertNotNull(evaluation.getOnboardingRejectionReasons());
        Assertions.assertTrue(evaluation.getOnboardingRejectionReasons().contains(expectedRejectionReason),
                "Expected rejection reason %s and obtained %s".formatted(expectedRejectionReason, evaluation.getOnboardingRejectionReasons()));


        assertEvaluationFields(evaluation, expectedInitiativeFieldFilled);
    }
    //endregion


    //region not valid useCases
    // all use cases configured must have a unique id recognized by the regexp errorUseCaseIdPatternMatch
    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();
    {
        String useCaseJsonNotExpected = "{\"initiativeId\":\"id_0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "Unexpected JSON", useCaseJsonNotExpected)
        ));

        String jsonNotValid = "{\"initiativeId\":\"id_1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "Unexpected JSON", jsonNotValid)
        ));

        final String failingOnboardingChecksUserId = "FAILING_ONBOARDING_CHECKS";
        String failingOnboardingChecks = TestUtils.jsonSerializer(
                OnboardingDTOFaker.mockInstanceBuilder(errorUseCases.size(), initiativesNumber)
                        .userId(failingOnboardingChecksUserId)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(onboardingCheckServiceSpy).check(Mockito.argThat(i->failingOnboardingChecksUserId.equals(i.getUserId())), Mockito.any());
                    return failingOnboardingChecks;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "Unexpected error", failingOnboardingChecks)
        ));

        final String failingAuthorityData = "FAILING_AUTHORITY_DATA";
        String failingAuthoritiesDataRetriever = TestUtils.jsonSerializer(
                OnboardingDTOFaker.mockInstanceBuilder(errorUseCases.size(), initiativesNumber)
                        .userId(failingAuthorityData)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(Mono.error(new RuntimeException("DUMMYEXCEPTION"))).when(authoritiesDataRetrieverServiceSpy).retrieve(Mockito.argThat(i->failingAuthorityData.equals(i.getUserId())), Mockito.any());
                    return failingAuthoritiesDataRetriever;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "An error occurred handling onboarding request", failingAuthoritiesDataRetriever)
        ));

        final String failingRuleEngineUserId = "FAILING_RULE_ENGINE";
        String failingRuleEngineUseCase = TestUtils.jsonSerializer(
                OnboardingDTOFaker.mockInstanceBuilder(errorUseCases.size(), initiativesNumber)
                        .userId(failingRuleEngineUserId)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(ruleEngineServiceSpy).applyRules(Mockito.argThat(i->failingRuleEngineUserId.equals(i.getUserId())), Mockito.any());
                    return failingRuleEngineUseCase;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "An error occurred handling onboarding request", failingRuleEngineUseCase)
        ));

        String failingBudgetReservation = TestUtils.jsonSerializer(
                OnboardingDTOFaker.mockInstanceBuilder(errorUseCases.size(), initiativesNumber)
                        .initiativeId(FAILING_BUDGET_RESERVATION_INITIATIVE_ID)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(Mono.error(new RuntimeException("DUMMYEXCEPTION"))).when(initiativeCountersRepositorySpy).reserveBudget(Mockito.eq(FAILING_BUDGET_RESERVATION_INITIATIVE_ID), Mockito.any());
                    return failingBudgetReservation;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "An error occurred handling onboarding request", failingBudgetReservation)
        ));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicAdmissibilityProcessorRequest, errorMessage, errorDescription, expectedPayload);
    }
    //endregion
}