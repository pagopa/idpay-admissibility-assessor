package it.gov.pagopa.admissibility.connector.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.admissibility.connector.event.consumer.BeneficiaryRuleBuilderConsumerConfigIntegrationTest;
import it.gov.pagopa.admissibility.connector.rest.PdndCreateTokenRestClient;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.dto.rule.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.service.ErrorNotifierServiceImpl;
import it.gov.pagopa.admissibility.service.UserFiscalCodeService;
import it.gov.pagopa.admissibility.service.onboarding.notifier.OnboardingNotifierService;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ContextConfiguration(classes = {AdmissibilityProcessorConfigTest.MediatorSpyConfiguration.class})
@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.service.onboarding.check.OnboardingInitiativeCheck=OFF",
        "logging.level.it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderServiceImpl=OFF",
        "logging.level.it.gov.pagopa.admissibility.service.onboarding.AdmissibilityEvaluatorMediatorServiceImpl=OFF",
})
class AdmissibilityProcessorConfigTest extends BaseAdmissibilityProcessorConfigTest {

    public static final String EXHAUSTED_INITIATIVE_ID = "EXHAUSTED_INITIATIVE_ID";
    public static final String FAILING_BUDGET_RESERVATION_INITIATIVE_ID = "id_7_FAILING_BUDGET_RESERVATION";
    public static final String RESIDENCE_INITIATIVE_ID = "RESIDENCE_INITIATIVE_ID";
    public static final String BIRTHDATE_INITIATIVE_ID = "BIRTHDATE_INITIATIVE_ID";
    public static final String ISEE_INITIATIVE_ID = "ISEE_INITIATIVE_ID";

    public static final String PDND_CLIENT_ID_RESIDENCE = "RESIDENCEINITIATIVECLIENTID";
    public static final String PDND_CLIENT_ID_ISEE = "ISEEINITIATIVECLIENTID";
    public static final String PDND_CLIENT_ID_BIRTHDATE = "BIRTHDATEINITIATIVECLIENTID";

    @SpyBean
    private OnboardingNotifierService onboardingNotifierServiceSpy;
    @SpyBean
    private UserFiscalCodeService userFiscalCodeServiceSpy;
    @SpyBean
    private PdndCreateTokenRestClient pdndCreateTokenRestClientSpy;

    @Value("${app.onboarding-request.max-retry}")
    private int maxRetry;

    private final int initiativesNumber = 7;

    @TestConfiguration
    static class MediatorSpyConfiguration extends BaseAdmissibilityProcessorConfigTest.MediatorSpyConfiguration{}

    @Test
    void testAdmissibilityOnboarding() throws IOException {
        int validOnboardings = 1000; // use even number
        int notValidOnboarding = errorUseCases.size();
        long maxWaitingMs = 30000;

        publishOnboardingRules(validOnboardings);

        List<Message<String>> onboardings = new ArrayList<>(buildValidPayloads(errorUseCases.size(), validOnboardings / 2, useCases));
        onboardings.addAll(IntStream.range(0, notValidOnboarding).mapToObj(i -> errorUseCases.get(i).getFirst().get()).map(p-> MessageBuilder.withPayload(p).build()).toList());
        onboardings.addAll(buildValidPayloads(errorUseCases.size() + (validOnboardings / 2) + notValidOnboarding, validOnboardings / 2, useCases));

        long timePublishOnboardingStart = System.currentTimeMillis();
        onboardings.forEach(i -> publishIntoEmbeddedKafka(topicAdmissibilityProcessorRequest, null, i));
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeConsumerResponse = System.currentTimeMillis();
        List<ConsumerRecord<String, String>> payloadConsumed = consumeMessages(topicAdmissibilityProcessorOutcome, validOnboardings, maxWaitingMs);
        long timeEnd = System.currentTimeMillis();

        long timeConsumerResponseEnd = timeEnd - timeConsumerResponse;
        Assertions.assertEquals(validOnboardings, payloadConsumed.size());

        for (ConsumerRecord<String, String> p : payloadConsumed) {
            EvaluationCompletedDTO evaluation = objectMapper.readValue(p.value(), EvaluationCompletedDTO.class);
            checkResponse(evaluation, useCases);
        }

        checkPdndAccessTokenInvocations();
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

        checkOffsets(onboardings.size(), validOnboardings, topicAdmissibilityProcessorOutcome);
    }

    private void checkPdndAccessTokenInvocations() {
        Map<String, Long> pdndClientIdsInvocations = Mockito.mockingDetails(pdndCreateTokenRestClientSpy).getInvocations().stream()
                .map(i -> i.getArgument(0, ApiKeysPDND.class))
                .collect(Collectors.groupingBy(ApiKeysPDND::getApiKeyClientId, Collectors.counting()));

        Assertions.assertEquals(Set.of(
                        PDND_CLIENT_ID_ISEE,
                        PDND_CLIENT_ID_RESIDENCE,
                        PDND_CLIENT_ID_BIRTHDATE
                ),
                pdndClientIdsInvocations.keySet());

        pdndClientIdsInvocations.forEach((clientId, invocations) ->
                Assertions.assertTrue(
                        invocations >= 1 && invocations < 4,
                        "Unexpected number of ClientId %s invocations".formatted(clientId)));
    }

    private void publishOnboardingRules(int onboardingsNumber) {
        int[] expectedRules = {0};
        Stream.concat(IntStream.range(0, initiativesNumber + 2)
                                .mapToObj(i -> {
                                    final Initiative2BuildDTO initiative = Initiative2BuildDTOFaker.mockInstance(i);

                                    BigDecimal budget;
                                    if (initiative.getInitiativeId().endsWith("_" + initiativesNumber)) {
                                        initiative.setInitiativeId(EXHAUSTED_INITIATIVE_ID);
                                        budget = BigDecimal.ZERO;
                                    } else if (initiative.getInitiativeId().endsWith("_" + (initiativesNumber + 1))) {
                                        initiative.setInitiativeId(FAILING_BUDGET_RESERVATION_INITIATIVE_ID);
                                        budget = BigDecimal.TEN;
                                    } else {
                                        budget = initiative.getGeneral().getBeneficiaryBudget().multiply(BigDecimal.valueOf(onboardingsNumber));
                                    }
                                    initiative.getGeneral().setBudget(budget);

                                    return initiative;
                                }),

                        Stream.of(
                                Initiative2BuildDTOFaker.mockInstanceBuilder(-1)
                                        .initiativeId(ISEE_INITIATIVE_ID)
                                        .initiativeName("ISEE_INITIATIVE_NAME")
                                        .beneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                                                .automatedCriteria(List.of(
                                                        AutomatedCriteriaDTO.builder()
                                                                .authority("AUTH1")
                                                                .code(CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE)
                                                                .operator(FilterOperator.GT)
                                                                .value("10000")
                                                                .iseeTypes(List.of(IseeTypologyEnum.ORDINARIO))
                                                                .build()
                                                ))
                                                .apiKeyClientId(Initiative2BuildDTOFaker.encrypt(PDND_CLIENT_ID_ISEE))
                                                .apiKeyClientAssertion(Initiative2BuildDTOFaker.encrypt(
                                                        Initiative2BuildDTOFaker.getClientAssertion(
                                                                "ISEEINITIATIVECLIENTASSERTIONFIRSTELEMENT",
                                                                Initiative2BuildDTOFaker.getAgidTokenPayload("ISEEINITIATIVECLIENTASSERTIONFIRSTELEMENT"),
                                                                "ISEEINITIATIVECLIENTASSERTIONLASTELEMENT"
                                                        )
                                                ))
                                                .build())
                                        .build(),

                                Initiative2BuildDTOFaker.mockInstanceBuilder(-1)
                                        .initiativeId(RESIDENCE_INITIATIVE_ID)
                                        .initiativeName("RESIDENCE_INITIATIVE_NAME")
                                        .beneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                                                .automatedCriteria(List.of(
                                                        AutomatedCriteriaDTO.builder()
                                                                .authority("AUTH1")
                                                                .code(CriteriaCodeConfigFaker.CRITERIA_CODE_RESIDENCE)
                                                                .field("city")
                                                                .operator(FilterOperator.EQ)
                                                                .value("Rome")
                                                                .build()
                                                ))
                                                .apiKeyClientId(Initiative2BuildDTOFaker.encrypt(PDND_CLIENT_ID_RESIDENCE))
                                                .apiKeyClientAssertion(Initiative2BuildDTOFaker.encrypt(
                                                        Initiative2BuildDTOFaker.getClientAssertion(
                                                                "RESIDENCEINITIATIVECLIENTASSERTIONFIRSTELEMENT",
                                                                Initiative2BuildDTOFaker.getAgidTokenPayload("RESIDENCEINITIATIVECLIENTASSERTIONFIRSTELEMENT"),
                                                                "RESIDENCEINITIATIVECLIENTASSERTIONLASTELEMENT"
                                                        )
                                                ))
                                                .build())
                                        .build(),

                                Initiative2BuildDTOFaker.mockInstanceBuilder(-1)
                                        .initiativeId(BIRTHDATE_INITIATIVE_ID)
                                        .initiativeName("BIRTHDATE_INITIATIVE_NAME")
                                        .beneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                                                .automatedCriteria(List.of(
                                                        AutomatedCriteriaDTO.builder()
                                                                .authority("AUTH1")
                                                                .code(CriteriaCodeConfigFaker.CRITERIA_CODE_BIRTHDATE)
                                                                .field("age")
                                                                .operator(FilterOperator.LT)
                                                                .value("10")
                                                                .build()
                                                ))
                                                .apiKeyClientId(Initiative2BuildDTOFaker.encrypt(PDND_CLIENT_ID_BIRTHDATE))
                                                .apiKeyClientAssertion(Initiative2BuildDTOFaker.encrypt(
                                                        Initiative2BuildDTOFaker.getClientAssertion(
                                                                "BIRTHDATEINITIATIVECLIENTASSERTIONFIRSTELEMENT",
                                                                Initiative2BuildDTOFaker.getAgidTokenPayload("BIRTHDATEINITIATIVECLIENTASSERTIONFIRSTELEMENT"),
                                                                "BIRTHDATEINITIATIVECLIENTASSERTIONLASTELEMENT"
                                                        )
                                                ))
                                                .build())
                                        .build(),

                                Initiative2BuildDTOFaker.mockInstanceBuilder(-1)
                                        .initiativeId("NEVERSELECTEDINITIATIVE")
                                        .initiativeName("NEVERSELECTEDINITIATIVE_NAME")
                                        .beneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                                                .automatedCriteria(List.of(
                                                        AutomatedCriteriaDTO.builder()
                                                                .authority("AUTH1")
                                                                .code(CriteriaCodeConfigFaker.CRITERIA_CODE_RESIDENCE)
                                                                .field("city")
                                                                .operator(FilterOperator.EQ)
                                                                .value("Rome")
                                                                .build()
                                                ))
                                                .apiKeyClientId(Initiative2BuildDTOFaker.encrypt("NEVERSELECTEDINITIATIVECLIENTID"))
                                                .apiKeyClientAssertion(Initiative2BuildDTOFaker.encrypt(
                                                        Initiative2BuildDTOFaker.getClientAssertion(
                                                                "NEVERSELECTEDINITIATIVECLIENTASSERTIONFIRSTELEMENT",
                                                                Initiative2BuildDTOFaker.getAgidTokenPayload("NEVERSELECTEDINITIATIVECLIENTASSERTIONFIRSTELEMENT"),
                                                                "NEVERSELECTEDINITIATIVECLIENTASSERTIONLASTELEMENT"
                                                        )
                                                ))
                                                .build())
                                        .build()
                        )
                )
                .peek(i -> expectedRules[0] += i.getBeneficiaryRule().getAutomatedCriteria().size())
                .forEach(i -> publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, null, null, i));

        BeneficiaryRuleBuilderConsumerConfigIntegrationTest.waitForKieContainerBuild(expectedRules[0], onboardingContextHolderServiceSpy);
    }

    private OnboardingDTO.OnboardingDTOBuilder buildOnboardingRequestCachedBuilder(Integer bias) {
        return OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                .isee(BigDecimal.valueOf(20))
                .birthDate(new BirthDate("1990", LocalDate.now().getYear() - 1990));
    }

    //region useCases
    private final List<OnboardingUseCase<EvaluationCompletedDTO>> useCases = List.of(
            // useCase 0: successful use case where AutomatedCriteria were cached
            OnboardingUseCase.withJustPayload(
                    bias -> buildOnboardingRequestCachedBuilder(bias)
                            .build(),
                    evaluation -> {
                        Assertions.assertEquals(Collections.emptyList(), evaluation.getOnboardingRejectionReasons());
                        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_OK, evaluation.getStatus());
                        assertEvaluationFields(evaluation, true);

                        Mockito.verify(userFiscalCodeServiceSpy, Mockito.never()).getUserFiscalCode(evaluation.getUserId());
                    }
            ),

            // useCase 1: TC consensus fail
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .tc(false)
                            .build(),
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED)
                                    .code(OnboardingConstants.REJECTION_REASON_CONSENSUS_TC_FAIL)
                                    .build(),
                            true)
            ),

            // useCase 2: PDND consensuns fail
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .pdndAccept(false)
                            .build(),
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED)
                                    .code(OnboardingConstants.REJECTION_REASON_CONSENSUS_PDND_FAIL)
                                    .build()
                            , true)
            ),

            // self declaration fail
            // Handle multi and boolean criteria
            /*
            OnboardingUseCase.withJustPayload(
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
            */

            // useCase 3: No initiative
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .initiativeId("NOT_EXISTENT")
                            .tcAcceptTimestamp(LocalDateTime.now().withYear(1970))
                            .build(),
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                                    .code( OnboardingConstants.REJECTION_REASON_INVALID_INITIATIVE_ID_FAIL)
                                    .build()
                            , false)
            ),

            // useCase 4: TC acceptance timestamp fail
            OnboardingUseCase.withJustPayload(
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

            // useCase 5: TC criteria acceptance timestamp fail
            OnboardingUseCase.withJustPayload(
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

            // TODO test error when invoking PDND

            // useCase 6: AUTOMATED_CRITERIA fail due to ISEE
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .initiativeId(ISEE_INITIATIVE_ID)

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

            // TODO ISEE test when multiple Isee typologies

            // TODO test daily limit reached when invoking INPS

            // useCase 7: AUTOMATED_CRITERIA fail due to RESIDENCE
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .initiativeId(RESIDENCE_INITIATIVE_ID)

                            .build(),
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL)
                                    .code(OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted("RESIDENCE"))
                                    .authority("AGID")
                                    .authorityLabel("Agenzia per l'Italia Digitale")
                                    .build()
                            , true)
            ),

            // useCase 8: AUTOMATED_CRITERIA fail due to BIRTHDATE
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .initiativeId(BIRTHDATE_INITIATIVE_ID)

                            .build(),
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL)
                                    .code(OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted("BIRTHDATE"))
                                    .authority("AGID")
                                    .authorityLabel("Agenzia per l'Italia Digitale")
                                    .build()
                            , true)
            ),

            // TODO test daily limit reached when invoking ANPR

            // exhausted initiative budget
            OnboardingUseCase.withJustPayload(
                    bias -> buildOnboardingRequestCachedBuilder(bias)
                            .initiativeId(EXHAUSTED_INITIATIVE_ID)
                            .build(),
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.BUDGET_EXHAUSTED)
                                    .code(OnboardingConstants.REJECTION_REASON_INITIATIVE_BUDGET_EXHAUSTED)
                                    .build()
                            , true)
            ),

            // useCase 8: evaluation throws exception, but retry header expired
            new OnboardingUseCase<>(
                    bias -> {
                        OnboardingDTO out = OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber).build();

                        Mockito.doReturn(Mono.error(new RuntimeException("DUMMYEXCEPTION"))).when(authoritiesDataRetrieverServiceSpy).retrieve(
                                Mockito.argThat(i->out.getUserId().equals(i.getUserId())), Mockito.any(), Mockito.any());

                        return MessageBuilder.withPayload(out).setHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_RETRY, maxRetry+"").build();
                    },
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR)
                                    .code(OnboardingConstants.REJECTION_REASON_GENERIC_ERROR)
                                    .build()
                            , true)
            )

    );

    private void assertEvaluationFields(EvaluationCompletedDTO evaluation, boolean expectedInitiativeFieldFilled){
        Assertions.assertNotNull(evaluation.getUserId());
        Assertions.assertNotNull(evaluation.getInitiativeId());
        Assertions.assertNotNull(evaluation.getStatus());
        Assertions.assertNotNull(evaluation.getAdmissibilityCheckDate());
        Assertions.assertNotNull(evaluation.getOnboardingRejectionReasons());

        Assertions.assertNull(evaluation.getFamilyId());

        if(expectedInitiativeFieldFilled) {
            Assertions.assertNotNull(evaluation.getInitiativeName());
            Assertions.assertNotNull(evaluation.getOrganizationId());
        } else {
            Assertions.assertNull(evaluation.getInitiativeName());
            Assertions.assertNull(evaluation.getOrganizationId());
        }
    }

    private void checkKO(EvaluationCompletedDTO evaluation, OnboardingRejectionReason expectedRejectionReason, boolean expectedInitiativeFieldFilled) {
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_KO, evaluation.getStatus());
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
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[ADMISSIBILITY_ONBOARDING_REQUEST] Unexpected JSON", useCaseJsonNotExpected)
        ));

        String jsonNotValid = "{\"initiativeId\":\"id_1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[ADMISSIBILITY_ONBOARDING_REQUEST] Unexpected JSON", jsonNotValid)
        ));

        final String failingOnboardingChecksUserId = "FAILING_ONBOARDING_CHECKS";
        String failingOnboardingChecks = TestUtils.jsonSerializer(
                OnboardingDTOFaker.mockInstanceBuilder(errorUseCases.size(), initiativesNumber)
                        .userId(failingOnboardingChecksUserId)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(onboardingCheckServiceSpy).check(Mockito.argThat(i->failingOnboardingChecksUserId.equals(i.getUserId())), Mockito.any(), Mockito.any());
                    return failingOnboardingChecks;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[ADMISSIBILITY_ONBOARDING_REQUEST] An error occurred handling onboarding request", failingOnboardingChecks)
        ));

        final String failingAuthorityData = "FAILING_AUTHORITY_DATA";
        String failingAuthoritiesDataRetriever = TestUtils.jsonSerializer(
                OnboardingDTOFaker.mockInstanceBuilder(errorUseCases.size(), initiativesNumber)
                        .userId(failingAuthorityData)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(Mono.error(new RuntimeException("DUMMYEXCEPTION"))).when(authoritiesDataRetrieverServiceSpy).retrieve(Mockito.argThat(i->failingAuthorityData.equals(i.getUserId())), Mockito.any(), Mockito.any());
                    return failingAuthoritiesDataRetriever;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[ADMISSIBILITY_ONBOARDING_REQUEST] An error occurred handling onboarding request", failingAuthoritiesDataRetriever)
        ));

        final String failingRuleEngineUserId = "FAILING_RULE_ENGINE";
        String failingRuleEngineUseCase = TestUtils.jsonSerializer(
                buildOnboardingRequestCachedBuilder(errorUseCases.size())
                        .userId(failingRuleEngineUserId)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(ruleEngineServiceSpy).applyRules(Mockito.argThat(i->failingRuleEngineUserId.equals(i.getUserId())), Mockito.any());
                    return failingRuleEngineUseCase;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[ADMISSIBILITY_ONBOARDING_REQUEST] An error occurred handling onboarding request", failingRuleEngineUseCase)
        ));

        final String failingOnboardingPublishingUserId = "FAILING_ONBOARDING_PUBLISHING";
        OnboardingDTO onboardingFailinPublishing = buildOnboardingRequestCachedBuilder(errorUseCases.size())
                .userId(failingOnboardingPublishingUserId)
                .build();
        int onboardingFailinPublishingInitiativeId = errorUseCases.size()%initiativesNumber;
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(false).when(onboardingNotifierServiceSpy).notify(Mockito.argThat(i -> failingOnboardingPublishingUserId.equals(i.getUserId())));
                    return TestUtils.jsonSerializer(onboardingFailinPublishing);
                },
                errorMessage-> {
                    EvaluationCompletedDTO expectedEvaluationFailingPublishing = retrieveEvaluationDTOErrorUseCase(onboardingFailinPublishing, onboardingFailinPublishingInitiativeId);
                    checkErrorMessageHeaders(kafkaBootstrapServers,topicAdmissibilityProcessorOutcome,null, errorMessage, "[ONBOARDING_REQUEST] An error occurred while publishing the onboarding evaluation result", TestUtils.jsonSerializer(expectedEvaluationFailingPublishing),false, false, true);
                }
        ));

        final String exceptionWhenOnboardingPublishingUserId = "FAILING_REWARD_PUBLISHING_DUE_EXCEPTION";
        OnboardingDTO exceptionWhenOnboardingPublishing = buildOnboardingRequestCachedBuilder(errorUseCases.size())
                .userId(exceptionWhenOnboardingPublishingUserId)
                .build();
        int exceptionWhenOnboardingPublishingInitiativeId = errorUseCases.size()%initiativesNumber;
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new KafkaException()).when(onboardingNotifierServiceSpy).notify(Mockito.argThat(i -> exceptionWhenOnboardingPublishingUserId.equals(i.getUserId())));
                    return TestUtils.jsonSerializer(exceptionWhenOnboardingPublishing);
                },
                errorMessage-> {
                    EvaluationCompletedDTO expectedEvaluationFailingPublishing = retrieveEvaluationDTOErrorUseCase(exceptionWhenOnboardingPublishing, exceptionWhenOnboardingPublishingInitiativeId);
                    checkErrorMessageHeaders(kafkaBootstrapServers,topicAdmissibilityProcessorOutcome,null, errorMessage, "[ONBOARDING_REQUEST] An error occurred while publishing the onboarding evaluation result", TestUtils.jsonSerializer(expectedEvaluationFailingPublishing),false, false, true);
                }
        ));

        String failingBudgetReservation = TestUtils.jsonSerializer(
                buildOnboardingRequestCachedBuilder(errorUseCases.size())
                        .initiativeId(FAILING_BUDGET_RESERVATION_INITIATIVE_ID)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(Mono.error(new RuntimeException("DUMMYEXCEPTION"))).when(initiativeCountersRepositorySpy).reserveBudget(Mockito.eq(FAILING_BUDGET_RESERVATION_INITIATIVE_ID), Mockito.any());
                    return failingBudgetReservation;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[ADMISSIBILITY_ONBOARDING_REQUEST] An error occurred handling onboarding request", failingBudgetReservation)
        ));
    }

    private EvaluationCompletedDTO retrieveEvaluationDTOErrorUseCase(OnboardingDTO onboardingDTO, int bias) {
        Initiative2BuildDTO initiativeExceptionWhenOnboardingPublishing = Initiative2BuildDTOFaker.mockInstance(bias);
        return EvaluationCompletedDTO.builder()
                .userId(onboardingDTO.getUserId())
                .initiativeId(onboardingDTO.getInitiativeId())
                .initiativeName(initiativeExceptionWhenOnboardingPublishing.getInitiativeName())
                .initiativeEndDate(initiativeExceptionWhenOnboardingPublishing.getGeneral().getEndDate())
                .organizationId(initiativeExceptionWhenOnboardingPublishing.getOrganizationId())
                .status(OnboardingEvaluationStatus.ONBOARDING_OK)
                .onboardingRejectionReasons(Collections.emptyList())
                .beneficiaryBudget(initiativeExceptionWhenOnboardingPublishing.getGeneral().getBeneficiaryBudget())
                .build();
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(serviceBusServers, topicAdmissibilityProcessorRequest, "", errorMessage, errorDescription, expectedPayload,true,true,false);
    }
    //endregion

    protected void checkPayload(String errorMessage, String expectedPayload) {
        try {
            EvaluationCompletedDTO actual = objectMapper.readValue(errorMessage, EvaluationCompletedDTO.class);
            EvaluationCompletedDTO expected = objectMapper.readValue(expectedPayload, EvaluationCompletedDTO.class);

            TestUtils.checkNotNullFields(actual, "rankingValue", "familyId");
            Assertions.assertEquals(expected.getUserId(), actual.getUserId());
            Assertions.assertEquals(expected.getInitiativeId(), actual.getInitiativeId());
            Assertions.assertEquals(expected.getInitiativeName(), actual.getInitiativeName());
            Assertions.assertEquals(expected.getOrganizationId(),actual.getOrganizationId());
            Assertions.assertEquals(expected.getStatus(), actual.getStatus());
            Assertions.assertEquals(expected.getOnboardingRejectionReasons(), actual.getOnboardingRejectionReasons());
            Assertions.assertEquals(expected.getBeneficiaryBudget(), actual.getBeneficiaryBudget());
            Assertions.assertEquals(expected.getRankingValue(), actual.getRankingValue());
        } catch (JsonProcessingException e) {
            Assertions.fail("Error check in payload");
        }
    }
}