package it.gov.pagopa.admissibility.event.processor;

import com.azure.spring.messaging.AzureHeaders;
import com.azure.spring.messaging.checkpoint.Checkpointer;
import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.RankingRequestDTO;
import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.event.consumer.BeneficiaryRuleBuilderConsumerConfigIntegrationTest;
import it.gov.pagopa.admissibility.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.service.onboarding.*;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import it.gov.pagopa.admissibility.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@Slf4j
@TestPropertySource(properties = {
        "app.beneficiary-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.admissibility.service.build=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.onboarding=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.onboarding.AdmissibilityEvaluatorMediatorServiceImpl=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.BaseKafkaConsumer=WARN",
})
class AdmissibilityProcessorConfigRankingTest extends BaseIntegrationTest {
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
    @SpyBean
    private RankingNotifierService rankingNotifierServiceSpy;

    private final int initiativesNumber = 7;
    private static List<Checkpointer> checkpointers;

    @TestConfiguration
    static class MediatorSpyConfiguration {
        @SpyBean
        private AdmissibilityEvaluatorMediatorService admissibilityEvaluatorMediatorServiceSpy;

        @PostConstruct
        void init() {
            checkpointers = configureSpies();
        }

        private List<Checkpointer> configureSpies(){
            List<Checkpointer> checkpoints = Collections.synchronizedList(new ArrayList<>(1100));

            Mockito.doAnswer(args-> {
                        Flux<Message<String>> messageFlux = args.getArgument(0);
                        messageFlux = messageFlux.map(m -> {
                                    if(m.getHeaders().get(ServiceBusMessageHeaders.SCHEDULED_ENQUEUE_TIME) == null) { //TODO verify commit on reschedule message when PDND integration will be test
                                        Checkpointer mock = Mockito.mock(Checkpointer.class);
                                        Mockito.when(mock.success()).thenReturn(Mono.empty());
                                        checkpoints.add(mock);
                                        return MessageBuilder.withPayload(m.getPayload())
                                                .copyHeaders(m.getHeaders())
                                                .setHeader(AzureHeaders.CHECKPOINTER, mock)
                                                .build();
                                    }else {
                                        return  m;
                                    }
                                })
                                .name("spy");
                        admissibilityEvaluatorMediatorServiceSpy.execute(messageFlux);
                        return null;
                    })
                    .when(admissibilityEvaluatorMediatorServiceSpy).execute(Mockito.argThat(a -> !Scannable.from(a).name().equals("spy")));

            return  checkpoints;
        }
    }
    @Test
    void testAdmissibilityOnboarding() throws IOException {
        int validOnboardings = 30;// 1000; // use even number
        int notValidOnboarding = errorUseCases.size();
        long maxWaitingMs = 30000;

        publishOnboardingRules(validOnboardings);

        List<String> onboardings = new ArrayList<>(buildValidPayloads(errorUseCases.size(), validOnboardings / 2));
        onboardings.addAll(IntStream.range(0, notValidOnboarding).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        onboardings.addAll(buildValidPayloads(errorUseCases.size() + (validOnboardings / 2) + notValidOnboarding, validOnboardings / 2));

        long timePublishOnboardingStart = System.currentTimeMillis();
        onboardings.forEach(i -> publishIntoEmbeddedKafka(topicAdmissibilityProcessorRequest, null, null, i));
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeConsumerResponse = System.currentTimeMillis();
        List<ConsumerRecord<String, String>> payloadConsumed = consumeMessages(topicAdmissibilityProcessorOutRankingRequest, validOnboardings/useCases.size(), maxWaitingMs);
        long timeEnd = System.currentTimeMillis();

        long timeConsumerResponseEnd = timeEnd - timeConsumerResponse;
        Assertions.assertEquals(validOnboardings/useCases.size(), payloadConsumed.size());

        for (ConsumerRecord<String, String> p : payloadConsumed) {
            RankingRequestDTO rankingRequest = objectMapper.readValue(p.value(), RankingRequestDTO.class);
            checkResponse(rankingRequest);
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

        checkOffsets(onboardings.size(), validOnboardings);
    }

    //TODO change
    private void publishOnboardingRules(int onboardingsNumber) {
        int[] expectedRules = {0};
        IntStream.range(0, initiativesNumber)
                .mapToObj(i -> {
                    final Initiative2BuildDTO initiative = Initiative2BuildDTOFaker.mockInstanceBuilder(i)
                            .rankingInitiative(Boolean.TRUE)
                            .build();

                    BigDecimal budget = initiative.getGeneral().getBeneficiaryBudget().multiply(BigDecimal.valueOf(onboardingsNumber));

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

    private void checkResponse(RankingRequestDTO rankingRequest) {
        String userId = rankingRequest.getUserId();
        int biasRetrieve = Integer.parseInt(userId.substring(7));
        useCases.get(biasRetrieve % useCases.size()).getSecond().accept(rankingRequest);
    }

    //region useCases
    private final List<Pair<Function<Integer, OnboardingDTO>, Consumer<RankingRequestDTO>>> useCases = List.of(
            //successful case - coda ranking
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstance(bias, initiativesNumber),
                    rankingRequest -> {
                        Assertions.assertEquals(Collections.emptyList(), rankingRequest.getOnboardingRejectionReasons());
                        assertRankingRequestFields(rankingRequest, true);
                    }
            )
    );

    private void assertRankingRequestFields(RankingRequestDTO rankingRequest, boolean expectedInitiativeFieldFilled){
        Assertions.assertNotNull(rankingRequest.getUserId());
        Assertions.assertNotNull(rankingRequest.getInitiativeId());
        Assertions.assertNotNull(rankingRequest.getAdmissibilityCheckDate());
        Assertions.assertNotNull(rankingRequest.getOnboardingRejectionReasons());

        if(expectedInitiativeFieldFilled) {
            Assertions.assertNotNull(rankingRequest.getInitiativeName());
            Assertions.assertNotNull(rankingRequest.getOrganizationId());
        } else {
            Assertions.assertNull(rankingRequest.getInitiativeName());
            Assertions.assertNull(rankingRequest.getOrganizationId());
        }
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
                    Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(onboardingCheckServiceSpy).check(Mockito.argThat(i->failingOnboardingChecksUserId.equals(i.getUserId())), Mockito.any());
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
                OnboardingDTOFaker.mockInstanceBuilder(errorUseCases.size(), initiativesNumber)
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

        final String failingRankingPublishingUserId = "FAILING_ONBOARDING_PUBLISHING";
        OnboardingDTO rankingFailinPublishing = OnboardingDTOFaker.mockInstanceBuilder(errorUseCases.size(), initiativesNumber)
                .userId(failingRankingPublishingUserId)
                .build();
        int rankingFailingPublishingInitiativeId = errorUseCases.size()%initiativesNumber;
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(false).when(rankingNotifierServiceSpy).notify(Mockito.argThat(i -> failingRankingPublishingUserId.equals(i.getUserId())));
                    return TestUtils.jsonSerializer(rankingFailinPublishing);
                },
                errorMessage-> {
                    RankingRequestDTO expectedEvaluationFailingPublishing = retrieveEvaluationDTOErrorUseCase(rankingFailinPublishing, rankingFailingPublishingInitiativeId);
                    checkErrorMessageHeaders(kafkaBootstrapServers,topicAdmissibilityProcessorOutRankingRequest,null, errorMessage, "[ADMISSIBILITY] An error occurred while publishing the ranking request", TestUtils.jsonSerializer(expectedEvaluationFailingPublishing),false, false, true);
                }
        ));

        final String exceptionWhenRankingPublishingUserId = "FAILING_ONBOARDING_PUBLISHING_DUE_EXCEPTION";
        OnboardingDTO exceptionWhenRankingPublishing = OnboardingDTOFaker.mockInstanceBuilder(errorUseCases.size(), initiativesNumber)
                .userId(exceptionWhenRankingPublishingUserId)
                .build();
        int exceptionWhenRankingPublishingInitiativeId = errorUseCases.size()%initiativesNumber;
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new KafkaException()).when(rankingNotifierServiceSpy).notify(Mockito.argThat(i -> exceptionWhenRankingPublishingUserId.equals(i.getUserId())));
                    return TestUtils.jsonSerializer(exceptionWhenRankingPublishing);
                },
                errorMessage-> {
                    RankingRequestDTO expectedEvaluationFailingPublishing = retrieveEvaluationDTOErrorUseCase(exceptionWhenRankingPublishing, exceptionWhenRankingPublishingInitiativeId);
                    checkErrorMessageHeaders(kafkaBootstrapServers,topicAdmissibilityProcessorOutRankingRequest,null, errorMessage, "[ADMISSIBILITY] An error occurred while publishing the ranking request", TestUtils.jsonSerializer(expectedEvaluationFailingPublishing),false, false, true);
                }
        ));
    }

    private RankingRequestDTO retrieveEvaluationDTOErrorUseCase(OnboardingDTO onboardingDTO, int bias) {
        Initiative2BuildDTO initiativeExceptionWhenOnboardingPublishing = Initiative2BuildDTOFaker.mockInstance(bias);
        return RankingRequestDTO.builder()
                .userId(onboardingDTO.getUserId())
                .initiativeId(onboardingDTO.getInitiativeId())
                .initiativeName(initiativeExceptionWhenOnboardingPublishing.getInitiativeName())
                .initiativeEndDate(initiativeExceptionWhenOnboardingPublishing.getGeneral().getEndDate())
                .organizationId(initiativeExceptionWhenOnboardingPublishing.getOrganizationId())
                .onboardingRejectionReasons(Collections.emptyList())
                .beneficiaryBudget(initiativeExceptionWhenOnboardingPublishing.getGeneral().getBeneficiaryBudget())
                .serviceId(initiativeExceptionWhenOnboardingPublishing.getAdditionalInfo().getServiceId())
                .build();
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(serviceBusServers, topicAdmissibilityProcessorRequest, "", errorMessage, errorDescription, expectedPayload,true,true,false);
    }
    //endregion

    protected void checkOffsets(long expectedReadMessages, long exptectedPublishedResults) {
        Assertions.assertEquals(expectedReadMessages, checkpointers.size());
        checkpointers.forEach(checkpointer -> Mockito.verify(checkpointer).success());

        long timeCommitChecked = System.currentTimeMillis();
        final Map<TopicPartition, Long> destPublishedOffsets = checkPublishedOffsets(topicAdmissibilityProcessorOutRankingRequest, exptectedPublishedResults);
        long timePublishChecked = System.currentTimeMillis();
        System.out.printf("""
                        ************************
                        Time occurred to check published offset: %d millis
                        ************************
                        Source Topic Committed Offsets: %s
                        Dest Topic Published Offsets: %s
                        ************************
                        """,
                timePublishChecked - timeCommitChecked,
                expectedReadMessages,
                destPublishedOffsets
        );
    }

    protected void checkPayload(String errorMessage, String expectedPayload) {
        try {
            RankingRequestDTO actual = objectMapper.readValue(errorMessage, RankingRequestDTO.class);
            RankingRequestDTO expected = objectMapper.readValue(expectedPayload, RankingRequestDTO.class);

            TestUtils.checkNotNullFields(actual);
            Assertions.assertEquals(expected.getUserId(), actual.getUserId());
            Assertions.assertEquals(expected.getInitiativeId(), actual.getInitiativeId());
            Assertions.assertEquals(expected.getInitiativeName(), actual.getInitiativeName());
            Assertions.assertEquals(expected.getOrganizationId(),actual.getOrganizationId());
            Assertions.assertEquals(expected.getOnboardingRejectionReasons(), actual.getOnboardingRejectionReasons());
            Assertions.assertEquals(expected.getBeneficiaryBudget(), actual.getBeneficiaryBudget());
            Assertions.assertEquals(expected.getServiceId(),actual.getServiceId());
        } catch (JsonProcessingException e) {
            Assertions.fail("Error check in payload");
        }
    }
}