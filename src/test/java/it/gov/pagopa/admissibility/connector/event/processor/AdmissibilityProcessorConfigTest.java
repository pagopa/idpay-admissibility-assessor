package it.gov.pagopa.admissibility.connector.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.event.consumer.BeneficiaryRuleBuilderConsumerConfigIntegrationTest;
import it.gov.pagopa.admissibility.connector.soap.inps.service.IseeConsultationSoapClient;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.dto.rule.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.notifier.OnboardingNotifierService;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import it.gov.pagopa.common.mongo.MongoTestUtilitiesService;
import it.gov.pagopa.common.reactive.pdnd.service.PdndRestClient;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import it.gov.pagopa.common.utils.TestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ContextConfiguration(classes = {AdmissibilityProcessorConfigTest.MediatorSpyConfiguration.class})
@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.admissibility.service.onboarding.check.OnboardingInitiativeCheck=OFF",
        "logging.level.it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderServiceImpl=OFF",
        "logging.level.it.gov.pagopa.admissibility.service.onboarding.AdmissibilityEvaluatorMediatorServiceImpl=OFF",
        "logging.level.it.gov.pagopa.admissibility.connector.soap.inps.service.IseeConsultationSoapClientImpl=OFF",
        "logging.level.it.gov.pagopa.admissibility.connector.soap.inps.service.InpsDataRetrieverServiceImpl=OFF",
})
class AdmissibilityProcessorConfigTest extends BaseAdmissibilityProcessorConfigTest {

    public static final String INITIATIVEID_EXHAUSTED = "EXHAUSTED_INITIATIVE_ID";
    public static final String INITIATIVEID_FAILING_BUDGET_RESERVATION = "id_7_FAILING_BUDGET_RESERVATION";
    public static final String INITIATIVEID_RESIDENCE = "RESIDENCE_INITIATIVE_ID";
    public static final String INITIATIVEID_BIRTHDATE = "BIRTHDATE_INITIATIVE_ID";
    public static final String INITIATIVEID_ISEE = "ISEE_INITIATIVE_ID";
    public static final String INITIATIVEID_PDNDIVOKEERROR = "PDNDERRORUSECASE";

    public static final String PDND_CLIENT_ID_RESIDENCE = "CLIENTID_RESIDENCEINITIATIVE";
    public static final String PDND_CLIENT_ID_ISEE = "CLIENTID_ISEEINITIATIVE";
    public static final String PDND_CLIENT_ID_BIRTHDATE = "CLIENTID_BIRTHDATEINITIATIVE";
    public static final String PDND_CLIENT_ID_PDND_ERROR = "CLIENTID_PDNDERROR";

    @SpyBean
    private OnboardingNotifierService onboardingNotifierServiceSpy;
    @SpyBean
    private UserFiscalCodeService userFiscalCodeServiceSpy;
    @SpyBean
    private PdndRestClient pdndRestClientSpy;
    @SpyBean
    private IseeConsultationSoapClient iseeConsultationSoapClientSpy;

    @Autowired
    private PagoPaAnprPdndConfig pagoPaAnprPdndConfig;

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

        MongoTestUtilitiesService.startMongoCommandListener("ON-BOARDINGS");

        long timePublishOnboardingStart = System.currentTimeMillis();
        onboardings.forEach(i -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicAdmissibilityProcessorRequest, null, i));
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeConsumerResponse = System.currentTimeMillis();
        List<ConsumerRecord<String, String>> payloadConsumed = kafkaTestUtilitiesService.consumeMessages(topicAdmissibilityProcessorOutcome, validOnboardings, maxWaitingMs);
        long timeEnd = System.currentTimeMillis();

        MongoTestUtilitiesService.stopAndPrintMongoCommands();

        long timeConsumerResponseEnd = timeEnd - timeConsumerResponse;
        Assertions.assertEquals(validOnboardings, payloadConsumed.size());

        for (ConsumerRecord<String, String> p : payloadConsumed) {
            EvaluationCompletedDTO evaluation = objectMapper.readValue(p.value(), EvaluationCompletedDTO.class);
            checkResponse(evaluation, useCases);
        }

        //checkPdndAccessTokenInvocations(); TODO
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
        Map<String, Long> pdndClientIdsInvocations = Mockito.mockingDetails(pdndRestClientSpy).getInvocations().stream()
                .map(i -> i.getArgument(0, String.class))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        Assertions.assertEquals(Set.of(
                        pagoPaAnprPdndConfig.getClientId() // It should change based on initiative/authority
                ),
                pdndClientIdsInvocations.keySet());

        pdndClientIdsInvocations.forEach((clientId, invocations) ->
                Assertions.assertTrue(
                        invocations >= 1 && invocations < 10, //ideally it would be 1, but due to concurrency accesses, more than 1 requests could be performed
                        "Unexpected number of ClientId %s invocations: %d".formatted(clientId, invocations)));
    }

    private void publishOnboardingRules(int onboardingsNumber) {
        MongoTestUtilitiesService.startMongoCommandListener("RULE PUBLISHING");

        int[] expectedRules = {0};
        Stream.concat(IntStream.range(0, initiativesNumber + 2)
                                .mapToObj(i -> {
                                    final Initiative2BuildDTO initiative = Initiative2BuildDTOFaker.mockInstance(i);

                                    BigDecimal budget;
                                    if (initiative.getInitiativeId().endsWith("_" + initiativesNumber)) {
                                        initiative.setInitiativeId(INITIATIVEID_EXHAUSTED);
                                        budget = BigDecimal.ZERO;
                                    } else if (initiative.getInitiativeId().endsWith("_" + (initiativesNumber + 1))) {
                                        initiative.setInitiativeId(INITIATIVEID_FAILING_BUDGET_RESERVATION);
                                        budget = BigDecimal.TEN;
                                    } else {
                                        budget = initiative.getGeneral().getBeneficiaryBudget().multiply(BigDecimal.valueOf(onboardingsNumber));
                                    }
                                    initiative.getGeneral().setBudget(budget);

                                    return initiative;
                                }),

                        Stream.of(
                                Initiative2BuildDTOFaker.mockInstanceBuilder(-1)
                                        .initiativeId(INITIATIVEID_ISEE)
                                        .initiativeName("ISEE_INITIATIVE_NAME")
                                        .beneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                                                .automatedCriteria(List.of(
                                                        AutomatedCriteriaDTO.builder()
                                                                .authority("AUTH1")
                                                                .code(CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE)
                                                                .operator(FilterOperator.GT)
                                                                .value("10000")
                                                                .iseeTypes(List.of(IseeTypologyEnum.ORDINARIO))
                                                                .pdndConfig(new PdndInitiativeConfig(PDND_CLIENT_ID_ISEE, "KID", "PURPOSE_ID_ISEE"))
                                                                .build()
                                                ))
                                                .build())
                                        .build(),

                                Initiative2BuildDTOFaker.mockInstanceBuilder(-1)
                                        .initiativeId(INITIATIVEID_RESIDENCE)
                                        .initiativeName("RESIDENCE_INITIATIVE_NAME")
                                        .beneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                                                .automatedCriteria(List.of(
                                                        AutomatedCriteriaDTO.builder()
                                                                .authority("AUTH1")
                                                                .code(CriteriaCodeConfigFaker.CRITERIA_CODE_RESIDENCE)
                                                                .field("city")
                                                                .operator(FilterOperator.EQ)
                                                                .value("Rome")
                                                                .pdndConfig(new PdndInitiativeConfig(PDND_CLIENT_ID_RESIDENCE, "KID", "PURPOSE_ID_RESIDENCE"))
                                                                .build()
                                                ))
                                                .build())
                                        .build(),

                                Initiative2BuildDTOFaker.mockInstanceBuilder(-1)
                                        .initiativeId(INITIATIVEID_BIRTHDATE)
                                        .initiativeName("BIRTHDATE_INITIATIVE_NAME")
                                        .beneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                                                .automatedCriteria(List.of(
                                                        AutomatedCriteriaDTO.builder()
                                                                .authority("AUTH1")
                                                                .code(CriteriaCodeConfigFaker.CRITERIA_CODE_BIRTHDATE)
                                                                .field("age")
                                                                .operator(FilterOperator.LT)
                                                                .value("10")
                                                                .pdndConfig(new PdndInitiativeConfig(PDND_CLIENT_ID_BIRTHDATE, "KID", "PURPOSE_ID_BIRTHDATE"))
                                                                .build()
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
                                                                .pdndConfig(new PdndInitiativeConfig(PDND_CLIENT_ID_RESIDENCE, "KID", "PURPOSE_ID_RESIDENCE"))
                                                                .build()
                                                ))
                                                .build())
                                        .build(),

                                Initiative2BuildDTOFaker.mockInstanceBuilder(-1)
                                        .initiativeId(INITIATIVEID_PDNDIVOKEERROR)
                                        .initiativeName("PDNDIVOKEERROR_NAME")
                                        .beneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                                                .automatedCriteria(List.of(
                                                        AutomatedCriteriaDTO.builder()
                                                                .authority("AUTH1")
                                                                .code(CriteriaCodeConfigFaker.CRITERIA_CODE_RESIDENCE)
                                                                .field("city")
                                                                .operator(FilterOperator.EQ)
                                                                .value("Rome")
                                                                .pdndConfig(new PdndInitiativeConfig(PDND_CLIENT_ID_PDND_ERROR, "KID", "PURPOSEID"))
                                                                .build()
                                                ))
                                                .build())
                                        .build()
                        )
                )
                .peek(i -> expectedRules[0] += i.getBeneficiaryRule().getAutomatedCriteria().size())
                .forEach(i -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, null, null, i));

        BeneficiaryRuleBuilderConsumerConfigIntegrationTest.waitForKieContainerBuild(expectedRules[0], onboardingContextHolderServiceSpy);

        MongoTestUtilitiesService.stopAndPrintMongoCommands();
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
                    evaluation -> checkOk(evaluation, false)
            ),

            // useCase 1: TC consensus fail
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .tc(false)
                            .build(),
                    evaluation -> checkConsensusMissedKo(evaluation, OnboardingConstants.REJECTION_REASON_CONSENSUS_TC_FAIL)
            ),

            // useCase 2: PDND consensuns fail
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .pdndAccept(false)
                            .build(),
                    evaluation -> checkConsensusMissedKo(evaluation, OnboardingConstants.REJECTION_REASON_CONSENSUS_PDND_FAIL)
            ),

            // self declaration fail
            // Handle multi and boolean criteria
            /*
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .selfDeclarationList(Map.of("DUMMY", false))
                            .build(),
                    evaluation -> checkConsensusMissedKo(evaluation, OnboardingConstants.REJECTION_REASON_CONSENSUS_CHECK_SELF_DECLARATION_FAIL_FORMAT.formatted("DUMMY"))
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
                    evaluation -> checkInvalidRequestKo(evaluation, OnboardingConstants.REJECTION_REASON_TC_CONSENSUS_DATETIME_FAIL)
            ),

            // useCase 5: TC criteria acceptance timestamp fail
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .criteriaConsensusTimestamp(LocalDateTime.now().withYear(1970))
                            .build(),
                    evaluation -> checkInvalidRequestKo(evaluation, OnboardingConstants.REJECTION_REASON_CRITERIA_CONSENSUS_DATETIME_FAIL)
            ),

            // useCase 6: error when invoking PDND
            OnboardingUseCase.withJustPayload(
                    bias -> {
                        OnboardingDTO out = OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                                .initiativeId(INITIATIVEID_PDNDIVOKEERROR)
                                .build();

                        Mockito.doReturn(Mono.error(new RuntimeException("DUMMYEXCEPTION"))).when(pdndRestClientSpy)
                                .createToken(Mockito.eq(PDND_CLIENT_ID_PDND_ERROR), Mockito.anyString());

                        return out;
                    },
                    this::checkResidenceKo
            ),

            // useCase 7: AUTOMATED_CRITERIA fail due to ISEE:
            //    PDV will return CF_OK fiscalCode, for which INPS stub will return a ISEE of 10.000, actual initiative allow > 10.000
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .initiativeId(INITIATIVEID_ISEE)
                            .build(),
                    this::checkIseeKo
            ),

            // useCase 8: retry due to ISEE Esito KO, then ONBOARDING_OK
            OnboardingUseCase.withJustPayload(
                    bias -> {
                        OnboardingDTO out = OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                                .initiativeId(INITIATIVEID_ISEE)
                                .build();

                        AtomicBoolean isRetry = new AtomicBoolean(false);
                        Mockito.doAnswer(i -> isRetry.getAndSet(true)
                                        ? Mono.just("CF_INVALID_REQUEST")
                                        : Mono.just("CF_OK_15000"))
                                .when(userFiscalCodeServiceSpy)
                                .getUserFiscalCode(out.getUserId());

                        return out;
                    },
                    evaluation -> checkOk(evaluation, true)
            ),

            // useCase 9: AUTOMATED_CRITERIA fail due to no ISEE returned
            OnboardingUseCase.withJustPayload(
                    bias -> {
                        OnboardingDTO out = OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                                .initiativeId(INITIATIVEID_ISEE)
                                .build();

                        Mockito.doReturn(Mono.just("CF_INPS_MOCKED"))
                                .when(userFiscalCodeServiceSpy)
                                .getUserFiscalCode(out.getUserId());

                        Mockito.doReturn(Mono.just(new ConsultazioneIndicatoreResponseType()))
                                .when(iseeConsultationSoapClientSpy)
                                .getIsee("CF_INPS_MOCKED", IseeTypologyEnum.ORDINARIO);
                        return out;
                    },
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO)
                                    .code(OnboardingConstants.REJECTION_REASON_ISEE_TYPE_KO)
                                    .authority("INPS")
                                    .authorityLabel("Istituto Nazionale Previdenza Sociale")
                                    .detail("ISEE non disponibile")
                                    .build()
                            , true)
            ),

            // TODO ISEE test when multiple Isee typologies

            // TODO test daily limit reached when invoking INPS

            // useCase 10: AUTOMATED_CRITERIA fail due to RESIDENCE
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .initiativeId(INITIATIVEID_RESIDENCE)

                            .build(),
                    this::checkResidenceKo
            ),

            // TODO test when no RESIDENCE returned

            // useCase 11: AUTOMATED_CRITERIA fail due to BIRTHDATE
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .initiativeId(INITIATIVEID_BIRTHDATE)

                            .build(),
                    this::checkBirthDateKo
            ),

            // TODO test when no BIRTHDATE returned

            // TODO test daily limit reached when invoking ANPR

            // useCase 12: exhausted initiative budget
            OnboardingUseCase.withJustPayload(
                    bias -> buildOnboardingRequestCachedBuilder(bias)
                            .initiativeId(INITIATIVEID_EXHAUSTED)
                            .build(),
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.BUDGET_EXHAUSTED)
                                    .code(OnboardingConstants.REJECTION_REASON_INITIATIVE_BUDGET_EXHAUSTED)
                                    .build()
                            , true)
            ),

            // useCase 13: evaluation throws exception, but retry header expired
            new OnboardingUseCase<>(
                    bias -> {
                        OnboardingDTO out = OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber).build();

                        Mockito.doReturn(Mono.error(new RuntimeException("DUMMYEXCEPTION"))).when(authoritiesDataRetrieverServiceSpy).retrieve(
                                Mockito.argThat(i->out.getUserId().equals(i.getUserId())), Mockito.any(), Mockito.any());

                        return MessageBuilder.withPayload(out).setHeader(KafkaConstants.ERROR_MSG_HEADER_RETRY, maxRetry+"").build();
                    },
                    evaluation -> checkKO(evaluation,
                            OnboardingRejectionReason.builder()
                                    .type(OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR)
                                    .code(OnboardingConstants.REJECTION_REASON_GENERIC_ERROR)
                                    .build()
                            , true)
            )

    );

    private void checkInvalidRequestKo(EvaluationCompletedDTO evaluation, String code) {
        checkKO(evaluation,
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                        .code(code)
                        .build()
               , true);
    }

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

    private void checkOk(EvaluationCompletedDTO evaluation, boolean expectedPdndInvoked) {
        Assertions.assertEquals(Collections.emptyList(), evaluation.getOnboardingRejectionReasons());
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_OK, evaluation.getStatus());
        assertEvaluationFields(evaluation, true);

        if(expectedPdndInvoked){
            Mockito.verify(userFiscalCodeServiceSpy).getUserFiscalCode(evaluation.getUserId());
        } else {
            Mockito.verify(userFiscalCodeServiceSpy, Mockito.never()).getUserFiscalCode(evaluation.getUserId());
        }
    }

    private void checkKO(EvaluationCompletedDTO evaluation, OnboardingRejectionReason expectedRejectionReason, boolean expectedInitiativeFieldFilled) {
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_KO, evaluation.getStatus());
        Assertions.assertNotNull(evaluation.getOnboardingRejectionReasons());
        Assertions.assertTrue(evaluation.getOnboardingRejectionReasons().contains(expectedRejectionReason),
                "Expected rejection reason %s and obtained %s".formatted(expectedRejectionReason, evaluation.getOnboardingRejectionReasons()));


        assertEvaluationFields(evaluation, expectedInitiativeFieldFilled);
    }

    private void checkConsensusMissedKo(EvaluationCompletedDTO evaluation, String code) {
        checkKO(evaluation,
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED)
                        .code(code)
                        .build(),
                true);
    }

    private void checkIseeKo(EvaluationCompletedDTO evaluation) {
        checkKO(evaluation,
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL)
                        .code(OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted("ISEE"))
                        .authority("INPS")
                        .authorityLabel("Istituto Nazionale Previdenza Sociale")
                        .build()
                , true);
    }

    private void checkResidenceKo(EvaluationCompletedDTO evaluation) {
        checkKO(evaluation,
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL)
                        .code(OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted("RESIDENCE"))
                        .authority("AGID")
                        .authorityLabel("Agenzia per l'Italia Digitale")
                        .build()
                , true);
    }

    private void checkBirthDateKo(EvaluationCompletedDTO evaluation) {
        checkKO(evaluation,
                OnboardingRejectionReason.builder()
                        .type(OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL)
                        .code(OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted("BIRTHDATE"))
                        .authority("AGID")
                        .authorityLabel("Agenzia per l'Italia Digitale")
                        .build()
                , true);
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
                    checkErrorMessageHeaders(kafkaBootstrapServers,topicAdmissibilityProcessorOutcome,null, errorMessage, "[ONBOARDING_REQUEST] An error occurred while publishing the onboarding evaluation result", TestUtils.jsonSerializer(expectedEvaluationFailingPublishing),null, false, false);
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
                    checkErrorMessageHeaders(kafkaBootstrapServers,topicAdmissibilityProcessorOutcome,null, errorMessage, "[ONBOARDING_REQUEST] An error occurred while publishing the onboarding evaluation result", TestUtils.jsonSerializer(expectedEvaluationFailingPublishing),null, false, false);
                }
        ));

        String failingBudgetReservation = TestUtils.jsonSerializer(
                buildOnboardingRequestCachedBuilder(errorUseCases.size())
                        .initiativeId(INITIATIVEID_FAILING_BUDGET_RESERVATION)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(Mono.error(new RuntimeException("DUMMYEXCEPTION"))).when(initiativeCountersRepositorySpy).reserveBudget(Mockito.eq(INITIATIVEID_FAILING_BUDGET_RESERVATION), Mockito.any());
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
                .initiativeRewardType(initiativeExceptionWhenOnboardingPublishing.getInitiativeRewardType())
                .isLogoPresent(initiativeExceptionWhenOnboardingPublishing.getAdditionalInfo() != null && !StringUtils.isEmpty(initiativeExceptionWhenOnboardingPublishing.getAdditionalInfo().getLogoFileName()))
                .organizationId(initiativeExceptionWhenOnboardingPublishing.getOrganizationId())
                .organizationName(initiativeExceptionWhenOnboardingPublishing.getOrganizationName())
                .status(OnboardingEvaluationStatus.ONBOARDING_OK)
                .onboardingRejectionReasons(Collections.emptyList())
                .beneficiaryBudget(initiativeExceptionWhenOnboardingPublishing.getGeneral().getBeneficiaryBudget())
                .admissibilityCheckDate(LocalDateTime.now())
                .criteriaConsensusTimestamp(onboardingDTO.getCriteriaConsensusTimestamp())
                .build();
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(serviceBusServers, topicAdmissibilityProcessorRequest, "", errorMessage, errorDescription, expectedPayload,null,true,true);
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