package it.gov.pagopa.admissibility.connector.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.event.consumer.BeneficiaryRuleBuilderConsumerConfigIntegrationTest;
import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprC001RestClientImplIntegrationTest;
import it.gov.pagopa.admissibility.connector.soap.inps.service.IseeConsultationSoapClientImplIntegrationTest;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationCompletedDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.dto.rule.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
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
import org.mockito.stubbing.Answer;
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
        "logging.level.it.gov.pagopa.common.reactive.pdnd.service.BaseRestPdndServiceClient=OFF",
})
class AdmissibilityProcessorConfigTest extends BaseAdmissibilityProcessorConfigTest {
    public static final String CF_AUTHORITIES_DATA_ALLOWED = "CF_OK_2";
    public static final String CF_INVALID_REQUEST = AnprC001RestClientImplIntegrationTest.FISCAL_CODE_INVALIDREQUEST;
    public static final String CF_INPS_RETRY = IseeConsultationSoapClientImplIntegrationTest.FISCAL_CODE_RETRY;
    public static final String CF_INPS_UNEXPECTED_RESULTCODE = IseeConsultationSoapClientImplIntegrationTest.FISCAL_CODE_UNEXPECTED_RESULT_CODE;
    public static final String CF_NOT_FOUND = AnprC001RestClientImplIntegrationTest.FISCAL_CODE_NOTFOUND;
    public static final String CF_ANPR_TOO_MANY_REQUESTS = AnprC001RestClientImplIntegrationTest.FISCAL_CODE_TOOMANYREQUESTS;
    public static final String CF_INPS_TOO_MANY_REQUESTS = IseeConsultationSoapClientImplIntegrationTest.FISCAL_CODE_TOOMANYREQUESTS;

    private static final String INITIATIVEID_EXHAUSTED = "EXHAUSTED_INITIATIVE_ID";
    private static final String INITIATIVEID_FAILING_BUDGET_RESERVATION = "FAILING_BUDGET_RESERVATION";
    private static final String INITIATIVEID_COMPLETE = "COMPLETE_INITIATIVE_ID";
    private static final String INITIATIVEID_PDNDIVOKEERROR = "PDNDERRORUSECASE";
    private static final String INITIATIVEID_MULTIPLE_ISEE_TYPES = "MULTIPLE_ISEE_TYPES";

    private static final String PDND_CLIENT_ID_RESIDENCE = "CLIENTID_RESIDENCEINITIATIVE";
    private static final String PDND_CLIENT_ID_ISEE = "CLIENTID_ISEEINITIATIVE";
    private static final String PDND_CLIENT_ID_BIRTHDATE = "CLIENTID_BIRTHDATEINITIATIVE";
    private static final String PDND_CLIENT_ID_PDND_ERROR = "CLIENTID_PDNDERROR";

//region OnboardingRejectionReason constants
    public static final OnboardingRejectionReason ONBOARDING_REJECTION_REASON_ISEE_TYPE_KO = OnboardingRejectionReason.builder()
            .type(OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO)
            .code(OnboardingConstants.REJECTION_REASON_ISEE_TYPE_KO)
            .authority("INPS")
            .authorityLabel("Istituto Nazionale Previdenza Sociale")
            .detail("ISEE non disponibile")
            .build();
    public static final OnboardingRejectionReason ONBOARDING_REJECTION_REASON_RESIDENCE_KO = OnboardingRejectionReason.builder()
            .type(OnboardingRejectionReason.OnboardingRejectionReasonType.RESIDENCE_KO)
            .code(OnboardingConstants.REJECTION_REASON_RESIDENCE_KO)
            .authority("AGID")
            .authorityLabel("Agenzia per l'Italia Digitale")
            .detail("Residenza non disponibile")
            .build();
    public static final OnboardingRejectionReason ONBOARDING_REJECTION_REASON_BIRTHDATE_KO = OnboardingRejectionReason.builder()
            .type(OnboardingRejectionReason.OnboardingRejectionReasonType.BIRTHDATE_KO)
            .code(OnboardingConstants.REJECTION_REASON_BIRTHDATE_KO)
            .authority("AGID")
            .authorityLabel("Agenzia per l'Italia Digitale")
            .detail("Data di nascita non disponibile")
            .build();

    public static final OnboardingRejectionReason ONBOARDING_REJECTION_REASON_ISEE = OnboardingRejectionReason.builder()
            .type(OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL)
            .code(OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted("ISEE"))
            .authority("INPS")
            .authorityLabel("Istituto Nazionale Previdenza Sociale")
            .build();
    public static final OnboardingRejectionReason ONBOARDING_REJECTION_REASON_RESIDENCE = OnboardingRejectionReason.builder()
            .type(OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL)
            .code(OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted("RESIDENCE"))
            .authority("AGID")
            .authorityLabel("Agenzia per l'Italia Digitale")
            .build();
    public static final OnboardingRejectionReason ONBOARDING_REJECTION_REASON_BIRTHDATE = OnboardingRejectionReason.builder()
            .type(OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL)
            .code(OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted("BIRTHDATE"))
            .authority("AGID")
            .authorityLabel("Agenzia per l'Italia Digitale")
            .build();

    public static final OnboardingRejectionReason ONBOARDING_REJECTION_REASON_CONSENSUS_TC = buildOnboardingRejectionReasonConsensusMissed(OnboardingConstants.REJECTION_REASON_CONSENSUS_TC_FAIL);
    public static final OnboardingRejectionReason ONBOARDING_REJECTION_REASON_CONSENSUS_PDND = buildOnboardingRejectionReasonConsensusMissed(OnboardingConstants.REJECTION_REASON_CONSENSUS_PDND_FAIL);
    public static final OnboardingRejectionReason ONBOARDING_REJECTION_REASON_INVALID_REQUEST_INITIATIVE_NOT_EXISTS = buildOnboardingRejectionReasonInvalidRequest(OnboardingConstants.REJECTION_REASON_INVALID_INITIATIVE_ID_FAIL);
    public static final OnboardingRejectionReason ONBOARDING_REJECTION_REASON_INVALID_REQUEST_TC_OUT_OF_DATE = buildOnboardingRejectionReasonInvalidRequest(OnboardingConstants.REJECTION_REASON_TC_CONSENSUS_DATETIME_FAIL);
    public static final OnboardingRejectionReason ONBOARDING_REJECTION_REASON_INVALID_REQUEST_CONSENSUS_OUT_OF_DATE = buildOnboardingRejectionReasonInvalidRequest(OnboardingConstants.REJECTION_REASON_CRITERIA_CONSENSUS_DATETIME_FAIL);
    public static final OnboardingRejectionReason ONBOARDING_REJECTION_REASON_BUDGET_EXHAUSTED = OnboardingRejectionReason.builder()
            .type(OnboardingRejectionReason.OnboardingRejectionReasonType.BUDGET_EXHAUSTED)
            .code(OnboardingConstants.REJECTION_REASON_INITIATIVE_BUDGET_EXHAUSTED)
            .build();
    public static final OnboardingRejectionReason ONBOARDING_REJECTION_REASON_GENERIC_ERROR = OnboardingRejectionReason.builder()
            .type(OnboardingRejectionReason.OnboardingRejectionReasonType.TECHNICAL_ERROR)
            .code(OnboardingConstants.REJECTION_REASON_GENERIC_ERROR)
            .build();
//endregion

    @SpyBean
    private OnboardingNotifierService onboardingNotifierServiceSpy;
    @SpyBean
    private UserFiscalCodeService userFiscalCodeServiceSpy;
    @SpyBean
    private PdndRestClient pdndRestClientSpy;

    @Autowired
    private PagoPaAnprPdndConfig pagoPaAnprPdndConfig;

    @Value("${app.onboarding-request.max-retry}")
    private int maxRetry;
    private int expectedRescheduling = 0;

    private final Map<String, String> userId2CFMocks = new HashMap<>();
    private final Map<String, Answer<Mono<String>>> userId2CFAnswers = new HashMap<>();

    @TestConfiguration
    static class MediatorSpyConfiguration extends BaseAdmissibilityProcessorConfigTest.MediatorSpyConfiguration{}

    @Test
    void testAdmissibilityOnboarding() throws IOException {
        int validOnboardings = Math.max(10,useCases.size()); // use even number
        int notValidOnboarding = errorUseCases.size();
        long maxWaitingMs = 30000;

        publishOnboardingRules(validOnboardings);

        List<Message<String>> onboardings = new ArrayList<>(buildValidPayloads(errorUseCases.size(), validOnboardings / 2, useCases));
        onboardings.addAll(IntStream.range(0, notValidOnboarding).mapToObj(i -> errorUseCases.get(i).getFirst().get()).map(p-> MessageBuilder.withPayload(p).build()).toList());
        onboardings.addAll(buildValidPayloads(errorUseCases.size() + (validOnboardings / 2) + notValidOnboarding, validOnboardings / 2, useCases));

        Mockito.doAnswer(i -> {
                    String cf = i.getArgument(0);
                    String mockedCF = userId2CFMocks.get(cf);
                    if (mockedCF != null) {
                        return Mono.just(mockedCF);
                    } else {
                        Answer<Mono<String>> mockedAnswer = userId2CFAnswers.get(cf);
                        if (mockedAnswer != null) {
                            return mockedAnswer.answer(i);
                        } else {
                            return i.callRealMethod();
                        }
                    }
                }).when(userFiscalCodeServiceSpy)
                .getUserFiscalCode(Mockito.any());

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

        checkOffsets(onboardings.size() + expectedRescheduling, validOnboardings, topicAdmissibilityProcessorOutcome);
    }

    private void checkPdndAccessTokenInvocations() {
        Map<String, Long> pdndClientIdsInvocations = Mockito.mockingDetails(pdndRestClientSpy).getInvocations().stream()
                .map(i -> i.getArgument(0, String.class))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        Assertions.assertEquals(Set.of(
                        pagoPaAnprPdndConfig.getClientId() // It should change based on initiative/authority, actually using once for the entire application
                ),
                pdndClientIdsInvocations.keySet());

        pdndClientIdsInvocations.forEach((clientId, invocations) ->
                Assertions.assertTrue(
                        // ideally it would be 1, but due to concurrency accesses, more than 1 requests could be performed
                        // if we want to strengthen this behavior, let's consider to introduce the use of it.gov.pagopa.common.reactive.service.LockService
                        invocations >= 1 && invocations < 50,
                        "Unexpected number of ClientId %s invocations: %d".formatted(clientId, invocations)));
    }

    private void publishOnboardingRules(int validOnboardings) {
        MongoTestUtilitiesService.startMongoCommandListener("RULE PUBLISHING");

        int[] expectedRules = {0};
        Stream.of(
                        Initiative2BuildDTOFaker.mockInstanceBuilder(0, BigDecimal.ZERO)
                                .initiativeId(INITIATIVEID_EXHAUSTED)
                                .initiativeName("EXHAUSTED_INITIATIVE_NAME")
                                .build(),
                        Initiative2BuildDTOFaker.mockInstanceBuilder(1, BigDecimal.TEN)
                                .initiativeId(INITIATIVEID_FAILING_BUDGET_RESERVATION)
                                .initiativeName("FAILING_BUDGET_RESERVATION_INITIATIVE_NAME")
                                .build(),
                        Initiative2BuildDTOFaker.mockInstanceBuilder(2, Initiative2BuildDTOFaker.BENEFICIARY_BUDGET.multiply(BigDecimal.valueOf(validOnboardings)))
                                .initiativeId(INITIATIVEID_COMPLETE)
                                .initiativeName("COMPLETE_INITIATIVE_NAME")
                                .beneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                                        .automatedCriteria(List.of(
                                                AutomatedCriteriaDTO.builder()
                                                        .authority("AUTH1")
                                                        .code(CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE)
                                                        .operator(FilterOperator.GT)
                                                        .value("10000")
                                                        .iseeTypes(List.of(IseeTypologyEnum.ORDINARIO))
                                                        .pdndConfig(new PdndInitiativeConfig(PDND_CLIENT_ID_ISEE, "KID", "PURPOSE_ID_ISEE"))
                                                        .build(),
                                                AutomatedCriteriaDTO.builder()
                                                        .authority("AUTH1")
                                                        .code(CriteriaCodeConfigFaker.CRITERIA_CODE_RESIDENCE)
                                                        .field("city")
                                                        .operator(FilterOperator.EQ)
                                                        .value("Roma")
                                                        .pdndConfig(new PdndInitiativeConfig(PDND_CLIENT_ID_RESIDENCE, "KID", "PURPOSE_ID_RESIDENCE"))
                                                        .build(),
                                                AutomatedCriteriaDTO.builder()
                                                        .authority("AUTH1")
                                                        .code(CriteriaCodeConfigFaker.CRITERIA_CODE_BIRTHDATE)
                                                        .field("age")
                                                        .operator(FilterOperator.GT)
                                                        .value("200")
                                                        .pdndConfig(new PdndInitiativeConfig(PDND_CLIENT_ID_BIRTHDATE, "KID", "PURPOSE_ID_BIRTHDATE"))
                                                        .build()
                                        ))
                                        .build())
                                .build(),

                        Initiative2BuildDTOFaker.mockInstanceBuilder(3)
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

                        Initiative2BuildDTOFaker.mockInstanceBuilder(4)
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
                                .build(),


                        Initiative2BuildDTOFaker.mockInstanceBuilder(5, Initiative2BuildDTOFaker.BENEFICIARY_BUDGET.multiply(BigDecimal.valueOf(validOnboardings)))
                                .initiativeId(INITIATIVEID_MULTIPLE_ISEE_TYPES)
                                .initiativeName("MULTIPLE_ISEE_TYPES_INITIATIVE_NAME")
                                .beneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                                        .automatedCriteria(List.of(
                                                AutomatedCriteriaDTO.builder()
                                                        .authority("AUTH1")
                                                        .code(CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE)
                                                        .operator(FilterOperator.GT)
                                                        .value("10000")
                                                        .iseeTypes(List.of(IseeTypologyEnum.CORRENTE, IseeTypologyEnum.MINORENNE, IseeTypologyEnum.RESIDENZIALE, IseeTypologyEnum.ORDINARIO))
                                                        .pdndConfig(new PdndInitiativeConfig(PDND_CLIENT_ID_ISEE, "KID", "PURPOSE_ID_ISEE"))
                                                        .build()
                                        ))
                                        .build())
                                .build()
                )
                .peek(i -> expectedRules[0] += i.getBeneficiaryRule().getAutomatedCriteria().size())
                .forEach(i -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, null, null, i));

        BeneficiaryRuleBuilderConsumerConfigIntegrationTest.waitForKieContainerBuild(expectedRules[0], onboardingContextHolderServiceSpy);

        MongoTestUtilitiesService.stopAndPrintMongoCommands();
    }

    private OnboardingDTO.OnboardingDTOBuilder buildOnboardingRequestCachedBuilder(Integer bias) {
        return OnboardingDTOFaker.mockInstanceBuilder(bias, INITIATIVEID_COMPLETE)
                .isee(BigDecimal.valueOf(15_000))
                .residence(new Residence("058091", "ROMA", "RM", "ROMA", "LAZIO", "ITALIA"))
                .birthDate(new BirthDate("1790", LocalDate.now().getYear() - 1790));
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
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, INITIATIVEID_COMPLETE)
                            .tc(false)
                            .build(),
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_CONSENSUS_TC), true)
            ),

            // useCase 2: PDND consensuns fail
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, INITIATIVEID_COMPLETE)
                                .pdndAccept(false)
                                .build(),
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_CONSENSUS_PDND), true)
            ),

            // self declaration fail
            // Handle multi and boolean criteria
            /*
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, INITIATIVEID_COMPLETE)
                            .selfDeclarationList(Map.of("DUMMY", false))
                            .build(),
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_CONSENSUS_SELF_DECLARATION), true)
            ),
            */

            // useCase 3: No initiative
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, INITIATIVEID_COMPLETE)
                            .initiativeId("NOT_EXISTENT")
                            .tcAcceptTimestamp(LocalDateTime.now().withYear(1970))
                            .build(),
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_INVALID_REQUEST_INITIATIVE_NOT_EXISTS), false)
            ),

            // useCase 4: TC acceptance timestamp fail
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, INITIATIVEID_COMPLETE)
                            .tcAcceptTimestamp(LocalDateTime.now().withYear(1970))
                            .build(),
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_INVALID_REQUEST_TC_OUT_OF_DATE), true)
            ),

            // useCase 5: TC criteria acceptance timestamp fail
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, INITIATIVEID_COMPLETE)
                            .criteriaConsensusTimestamp(LocalDateTime.now().withYear(1970))
                            .build(),
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_INVALID_REQUEST_CONSENSUS_OUT_OF_DATE), true)
            ),

            // useCase -: error when invoking PDND
            // TODO actually not testable because the entire application will use the same PDNDInitiativeConfig not read from InitiativeConfig
//            OnboardingUseCase.withJustPayload(
//                    bias -> {
//                        OnboardingDTO out = OnboardingDTOFaker.mockInstance(bias, INITIATIVEID_PDNDIVOKEERROR);
//
//                        Mockito.doReturn(Mono.error(new RuntimeException("DUMMYEXCEPTION"))).when(pdndRestClientSpy)
//                                .createToken(Mockito.eq(PDND_CLIENT_ID_PDND_ERROR), Mockito.anyString());
//
//                        return out;
//                    },
//                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_RESIDENCE_KO), true)
//            ),

            // useCase 6: AUTOMATED_CRITERIA fails due to ISEE, RESIDENCE and BIRTHDATE:
            //    PDV will return CF_OK fiscalCode, for which:
            //       INPS stub will return a ISEE of 10.000, actual initiative allow > 10.000
            //       ANPR stub will return PAVULLO NEL FRIGNANO as RESIDENCE and 1990-01-01 as BIRTHDATE
            OnboardingUseCase.withJustPayload(
                    bias -> OnboardingDTOFaker.mockInstance(bias, INITIATIVEID_COMPLETE),
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_ISEE, ONBOARDING_REJECTION_REASON_RESIDENCE, ONBOARDING_REJECTION_REASON_BIRTHDATE), true)
            ),

            // useCase 7: ONBOARDING_OK invoking PDND services:
            //    PDV will return CF_OK_2 fiscalCode, for which:
            //       INPS stub will return a ISEE of 15.000, actual initiative allow > 10.000
            //       ANPR stub will return ROMA as RESIDENCE and 1790-01-01 as BIRTHDATE
            OnboardingUseCase.withJustPayload(
                    bias -> {
                        OnboardingDTO out = OnboardingDTOFaker.mockInstance(bias, INITIATIVEID_COMPLETE);

                        userId2CFMocks.put(out.getUserId(), CF_AUTHORITIES_DATA_ALLOWED);

                        return out;
                    },
                    evaluation -> checkOk(evaluation, true)
            ),

            // useCase 8: AUTOMATED_CRITERIA fail due to CF not found
            OnboardingUseCase.withJustPayload(
                    bias -> {
                        OnboardingDTO out = OnboardingDTOFaker.mockInstance(bias, INITIATIVEID_COMPLETE);

                        userId2CFMocks.put(out.getUserId(), CF_NOT_FOUND);

                        return out;
                    },
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_ISEE_TYPE_KO, ONBOARDING_REJECTION_REASON_RESIDENCE_KO, ONBOARDING_REJECTION_REASON_BIRTHDATE_KO), true)
            ),

            // useCase 9 AUTOMATED_CRITERIA fail due to invalid request
            OnboardingUseCase.withJustPayload(
                    bias -> {
                        OnboardingDTO out = OnboardingDTOFaker.mockInstance(bias, INITIATIVEID_COMPLETE);

                        userId2CFMocks.put(out.getUserId(), CF_INVALID_REQUEST);

                        return out;
                    },
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_ISEE_TYPE_KO, ONBOARDING_REJECTION_REASON_RESIDENCE_KO, ONBOARDING_REJECTION_REASON_BIRTHDATE_KO), true)
            ),

            // useCase 10: retry due to ISEE Esito KO, then ONBOARDING_KO due to ANPR (just INPS is invoked again, ANPR result will be cached and its first attempt will retrieve default values, not allowed by criteria)
            OnboardingUseCase.withJustPayload(
                    bias -> {
                        OnboardingDTO out = OnboardingDTOFaker.mockInstance(bias, INITIATIVEID_COMPLETE);

                        AtomicBoolean isRetry = new AtomicBoolean(false);
                        userId2CFAnswers.put(out.getUserId(),
                                i -> !isRetry.getAndSet(true)
                                        ? Mono.just(CF_INPS_RETRY)
                                        : Mono.just(CF_AUTHORITIES_DATA_ALLOWED)
                        );

                        expectedRescheduling++;

                        return out;
                    },
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_RESIDENCE, ONBOARDING_REJECTION_REASON_BIRTHDATE), true)
            ),

            // useCase 11: INPS unexpected result code
            OnboardingUseCase.withJustPayload(
                    bias -> {
                        OnboardingDTO out = OnboardingDTOFaker.mockInstance(bias, INITIATIVEID_COMPLETE);

                        userId2CFMocks.put(out.getUserId(), CF_INPS_UNEXPECTED_RESULTCODE);

                        return out;
                    },
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_ISEE_TYPE_KO), true)
            ),

            // useCase 12: multiple Isee typologies
            OnboardingUseCase.withJustPayload(
                    bias -> {
                        OnboardingDTO out = OnboardingDTOFaker.mockInstance(bias, INITIATIVEID_MULTIPLE_ISEE_TYPES);

                        userId2CFMocks.put(out.getUserId(), CF_AUTHORITIES_DATA_ALLOWED);

                        return out;
                    },
                    evaluation -> checkOk(evaluation, true)
            ),

            // useCase 13: daily limit reached when invoking INPS, then ONBOARDING_KO due to ANPR (just INPS is invoked again, ANPR result will be cached and its first attempt will retrieve default values, not allowed by criteria)
            OnboardingUseCase.withJustPayload(
                    bias -> {
                        OnboardingDTO out = OnboardingDTOFaker.mockInstance(bias, INITIATIVEID_COMPLETE);

                        AtomicBoolean isRetry = new AtomicBoolean(false);
                        userId2CFAnswers.put(out.getUserId(),
                                i -> !isRetry.getAndSet(true)
                                        ? Mono.just(CF_INPS_TOO_MANY_REQUESTS)
                                        : Mono.just(CF_AUTHORITIES_DATA_ALLOWED)
                        );

                        expectedRescheduling++;

                        return out;
                    },
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_RESIDENCE, ONBOARDING_REJECTION_REASON_BIRTHDATE), true)
            ),

            // useCase 14: daily limit reached when invoking ANPR, then ONBOARDING_KO due to INPS (just ANPR is invoked again, INPS result will be cached and its first attempt will retrieve default values, not allowed by criteria)
            OnboardingUseCase.withJustPayload(
                    bias -> {
                        OnboardingDTO out = OnboardingDTOFaker.mockInstance(bias, INITIATIVEID_COMPLETE);

                        AtomicBoolean isRetry = new AtomicBoolean(false);
                        userId2CFAnswers.put(out.getUserId(),
                                i -> !isRetry.getAndSet(true)
                                        ? Mono.just(CF_ANPR_TOO_MANY_REQUESTS)
                                        : Mono.just(CF_AUTHORITIES_DATA_ALLOWED)
                        );

                        expectedRescheduling++;

                        return out;
                    },
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_ISEE), true)
            ),

            // useCase 15: exhausted initiative budget
            OnboardingUseCase.withJustPayload(
                    bias -> buildOnboardingRequestCachedBuilder(bias)
                            .initiativeId(INITIATIVEID_EXHAUSTED)
                            .build(),
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_BUDGET_EXHAUSTED), true)
            ),

            // useCase 16: evaluation throws exception, but retry header expired
            new OnboardingUseCase<>(
                    bias -> {
                        OnboardingDTO out = OnboardingDTOFaker.mockInstance(bias, INITIATIVEID_COMPLETE);

                        Mockito.doReturn(Mono.error(new RuntimeException("DUMMYEXCEPTION"))).when(authoritiesDataRetrieverServiceSpy).retrieve(
                                Mockito.argThat(i->out.getUserId().equals(i.getUserId())), Mockito.any(), Mockito.any());

                        return MessageBuilder.withPayload(out).setHeader(KafkaConstants.ERROR_MSG_HEADER_RETRY, maxRetry+"").build();
                    },
                    evaluation -> checkKO(evaluation, List.of(ONBOARDING_REJECTION_REASON_GENERIC_ERROR), true)
            )

    );

    private static OnboardingRejectionReason buildOnboardingRejectionReasonInvalidRequest(String code) {
        return OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                .code(code)
                .build();
    }

    private static OnboardingRejectionReason buildOnboardingRejectionReasonConsensusMissed(String code) {
        return OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.CONSENSUS_MISSED)
                .code(code)
                .build();
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

    private void checkKO(EvaluationCompletedDTO evaluation, List<OnboardingRejectionReason> expectedRejectionReason, boolean expectedInitiativeFieldFilled) {
        Assertions.assertEquals(OnboardingEvaluationStatus.ONBOARDING_KO, evaluation.getStatus());
        Assertions.assertNotNull(evaluation.getOnboardingRejectionReasons());
        Assertions.assertEquals(expectedRejectionReason, evaluation.getOnboardingRejectionReasons());


        assertEvaluationFields(evaluation, expectedInitiativeFieldFilled);
    }

    //endregion


    //region not valid useCases
    // all use cases configured must have a unique id recognized by the regexp errorUseCaseIdPatternMatch
    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();
    {
        //errorUseCase 0
        String useCaseJsonNotExpected = "{\"userId\":\"userId_0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[ADMISSIBILITY_ONBOARDING_REQUEST] Unexpected JSON", useCaseJsonNotExpected)
        ));

        //errorUseCase 1
        String jsonNotValid = "{\"userId\":\"userId_1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[ADMISSIBILITY_ONBOARDING_REQUEST] Unexpected JSON", jsonNotValid)
        ));

        //errorUseCase 2
        final String failingOnboardingChecksUserId = "userId_2_FAILING_ONBOARDING_CHECKS";
        String failingOnboardingChecks = TestUtils.jsonSerializer(
                OnboardingDTOFaker.mockInstanceBuilder(errorUseCases.size(), INITIATIVEID_COMPLETE)
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

        //errorUseCase 3
        final String failingAuthorityDataUserId = "userId_3_FAILING_AUTHORITY_DATA";
        String failingAuthoritiesDataRetriever = TestUtils.jsonSerializer(
                OnboardingDTOFaker.mockInstanceBuilder(errorUseCases.size(), INITIATIVEID_COMPLETE)
                        .userId(failingAuthorityDataUserId)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(Mono.error(new RuntimeException("DUMMYEXCEPTION"))).when(authoritiesDataRetrieverServiceSpy).retrieve(Mockito.argThat(i->failingAuthorityDataUserId.equals(i.getUserId())), Mockito.any(), Mockito.any());
                    return failingAuthoritiesDataRetriever;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[ADMISSIBILITY_ONBOARDING_REQUEST] An error occurred handling onboarding request", failingAuthoritiesDataRetriever)
        ));

        //errorUseCase 4
        final String failingRuleEngineUserId = "userId_4_FAILING_RULE_ENGINE";
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

        //errorUseCase 5
        final String failingOnboardingPublishingUserId = "userId_5_FAILING_ONBOARDING_PUBLISHING";
        OnboardingDTO onboardingFailingPublishing = buildOnboardingRequestCachedBuilder(errorUseCases.size())
                .userId(failingOnboardingPublishingUserId)
                .build();
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(false).when(onboardingNotifierServiceSpy).notify(Mockito.argThat(i -> failingOnboardingPublishingUserId.equals(i.getUserId())));
                    return TestUtils.jsonSerializer(onboardingFailingPublishing);
                },
                errorMessage-> {
                    EvaluationCompletedDTO expectedEvaluationFailingPublishing = retrieveEvaluationDTOErrorUseCase(onboardingFailingPublishing);
                    checkErrorMessageHeaders(kafkaBootstrapServers,topicAdmissibilityProcessorOutcome,null, errorMessage, "[ONBOARDING_REQUEST] An error occurred while publishing the onboarding evaluation result", TestUtils.jsonSerializer(expectedEvaluationFailingPublishing),null, false, false);
                }
        ));

        //errorUseCase 6
        final String exceptionWhenOnboardingPublishingUserId = "userId_6_FAILING_REWARD_PUBLISHING_DUE_EXCEPTION";
        OnboardingDTO exceptionWhenOnboardingPublishing = buildOnboardingRequestCachedBuilder(errorUseCases.size())
                .userId(exceptionWhenOnboardingPublishingUserId)
                .build();
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new KafkaException()).when(onboardingNotifierServiceSpy).notify(Mockito.argThat(i -> exceptionWhenOnboardingPublishingUserId.equals(i.getUserId())));
                    return TestUtils.jsonSerializer(exceptionWhenOnboardingPublishing);
                },
                errorMessage-> {
                    EvaluationCompletedDTO expectedEvaluationFailingPublishing = retrieveEvaluationDTOErrorUseCase(exceptionWhenOnboardingPublishing);
                    checkErrorMessageHeaders(kafkaBootstrapServers,topicAdmissibilityProcessorOutcome,null, errorMessage, "[ONBOARDING_REQUEST] An error occurred while publishing the onboarding evaluation result", TestUtils.jsonSerializer(expectedEvaluationFailingPublishing),null, false, false);
                }
        ));

        //errorUseCase 7
        String failingBudgetReservation = TestUtils.jsonSerializer(
                buildOnboardingRequestCachedBuilder(errorUseCases.size())
                        .initiativeId(INITIATIVEID_FAILING_BUDGET_RESERVATION)
                        .userId("userId_7_FAILING_BUDGET_RESERVATION")
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

    private EvaluationCompletedDTO retrieveEvaluationDTOErrorUseCase(OnboardingDTO onboardingDTO) {
        Initiative2BuildDTO initiativeExceptionWhenOnboardingPublishing = Initiative2BuildDTOFaker.mockInstance(2); // COMPLETE_INITIATIVEID bias
        return EvaluationCompletedDTO.builder()
                .userId(onboardingDTO.getUserId())
                .initiativeId(onboardingDTO.getInitiativeId())
                .initiativeName("COMPLETE_INITIATIVE_NAME")
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