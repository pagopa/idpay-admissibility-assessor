package it.gov.pagopa.admissibility.connector.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.admissibility.connector.event.consumer.BeneficiaryRuleBuilderConsumerConfigIntegrationTest;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.admissibility.dto.onboarding.*;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import it.gov.pagopa.admissibility.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.admissibility.model.InitiativeCounters;
import it.gov.pagopa.admissibility.model.OnboardingFamilies;
import it.gov.pagopa.admissibility.service.onboarding.notifier.OnboardingRescheduleService;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.FamilyDataRetrieverService;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.FamilyDataRetrieverServiceImpl;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.mongo.MongoTestUtilitiesService;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ContextConfiguration(inheritInitializers = false)
class AdmissibilityProcessorConfigFamilyTest extends BaseAdmissibilityProcessorConfigTest {

    @TestConfiguration
    static class MediatorSpyConfiguration extends BaseAdmissibilityProcessorConfigTest.MediatorSpyConfiguration {}

    private List<Initiative2BuildDTO> publishedInitiatives;

    private final int onboardingFamilies =50;
    private final int membersPerFamily=3;

    private int expectedOnboardingKoFamilies=0;
    private int expectedFamilyRetrieveKo=0;

    private int expectedOnboardedFamilies;

    @SpyBean
    private OnboardingRescheduleService rescheduleServiceSpy;
    @SpyBean
    private FamilyDataRetrieverService familyDataRetrieverServiceSpy;

    @Autowired
    private OnboardingFamiliesRepository onboardingFamiliesRepository;
    @Autowired
    private InitiativeCountersRepository initiativeCountersRepository;
    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @Test
    void testFamilyAdmissibilityOnboarding() throws IOException {
        long maxWaitingMs = 30000;

        publishOnboardingRules();

        List<Message<String>> onboardings = new ArrayList<>(buildValidPayloads(0, onboardingFamilies, useCases));

        storeInitiativeCountersInitialState();

        int expectedRequestsPerInitiative = onboardingFamilies * membersPerFamily;
        int expectedPublishedMessages = expectedRequestsPerInitiative * publishedInitiatives.size();
        int expectedEvaluationCompletedMessages =
                expectedRequestsPerInitiative
                + expectedOnboardingKoFamilies * membersPerFamily // rankingKO are published also here
                + (onboardingFamilies - expectedOnboardingKoFamilies) * (membersPerFamily - 1); // DEMANDED for each other member when ONBOARDING_OK

        expectedOnboardedFamilies = onboardingFamilies - expectedFamilyRetrieveKo;

        MongoTestUtilitiesService.startMongoCommandListener("ON-BOARDINGS");

        long timePublishOnboardingStart = System.currentTimeMillis();
        onboardings.forEach(i -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicAdmissibilityProcessorRequest, null, i));
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeConsumerResponse = System.currentTimeMillis();
        List<ConsumerRecord<String, String>> rankingRequestPayloadConsumed = kafkaTestUtilitiesService.consumeMessages(topicAdmissibilityProcessorOutRankingRequest, expectedRequestsPerInitiative, maxWaitingMs);
        List<ConsumerRecord<String, String>> evaluationOutcomePayloadConsumed = kafkaTestUtilitiesService.consumeMessages(topicAdmissibilityProcessorOutcome, expectedEvaluationCompletedMessages, maxWaitingMs);
        long timeEnd = System.currentTimeMillis();

        long timeConsumerResponseEnd = timeEnd - timeConsumerResponse;

        MongoTestUtilitiesService.stopAndPrintMongoCommands();

        checkResponses(expectedRequestsPerInitiative, rankingRequestPayloadConsumed, RankingRequestDTO.class);
        checkResponses(expectedEvaluationCompletedMessages, evaluationOutcomePayloadConsumed, EvaluationCompletedDTO.class);

        checkStoredOnboardingFamilies();
        checkInitiativeCounters();

        long rescheduled = Mockito.mockingDetails(rescheduleServiceSpy).getInvocations().stream()
                .peek(i -> {
                    OnboardingDTO rescheduledRequest = i.getArgument(0, OnboardingDTO.class);
                    Assertions.assertNotNull(rescheduledRequest.getFamily());
                }).count();

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d members per %d families per %d initiatives) onboarding request messages: %d millis
                        Rescheduled %d messages because IN_PROGRESS
                        Time spent to consume onboarding responses: %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                expectedPublishedMessages,
                membersPerFamily,
                publishedInitiatives.size(),
                onboardingFamilies,
                timePublishingOnboardingRequest,
                rescheduled,
                timeConsumerResponseEnd,
                timeEnd - timePublishOnboardingStart
        );

        checkOffsets(onboardings.size() + rescheduled, expectedRequestsPerInitiative, topicAdmissibilityProcessorOutRankingRequest);
        checkOffsets(onboardings.size() + rescheduled, expectedEvaluationCompletedMessages, topicAdmissibilityProcessorOutcome);
    }

    private void publishOnboardingRules() {
        MongoTestUtilitiesService.startMongoCommandListener("RULE PUBLISHING");

        int[] expectedRules = {0};
        publishedInitiatives = IntStream.range(0, 2)
                .mapToObj(i -> {
                    final Initiative2BuildDTO initiative = Initiative2BuildDTOFaker.mockInstanceBuilder(i)
                            .build();

                    initiative.getGeneral().setRankingEnabled(i % 2 == 0);

                    initiative.setInitiativeId(initiative.getGeneral().isRankingEnabled()? "RANKINGINITIATIVEID":"INITIATIVEID");

                    initiative.getGeneral().setBeneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF);
                    initiative.getBeneficiaryRule().getAutomatedCriteria().get(0).setOrderDirection(Sort.Direction.ASC);
                    initiative.getBeneficiaryRule().setAutomatedCriteria(List.of(initiative.getBeneficiaryRule().getAutomatedCriteria().get(0)));

                    BigDecimal budget = initiative.getGeneral().getBeneficiaryBudget().multiply(BigDecimal.valueOf(onboardingFamilies));

                    initiative.getGeneral().setBudget(budget);

                    return initiative;
                })
                .peek(i -> expectedRules[0] += i.getBeneficiaryRule().getAutomatedCriteria().size())
                .peek(i -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, null, null, i))
                .toList();

        BeneficiaryRuleBuilderConsumerConfigIntegrationTest.waitForKieContainerBuild(expectedRules[0], onboardingContextHolderServiceSpy);

        MongoTestUtilitiesService.stopAndPrintMongoCommands();
    }

    @Override
    protected <T extends EvaluationDTO> List<Message<String>> buildValidPayloads(int bias, int validOnboardings, List<OnboardingUseCase<T>> useCases) {
        return super.buildValidPayloads(bias, validOnboardings, useCases).stream()
                .flatMap(message -> publishedInitiatives.stream()
                        .flatMap(initiative ->
                                IntStream.range(0, membersPerFamily)
                                        .mapToObj(i ->
                                                MessageBuilder.withPayload(message.getPayload()
                                                                .replace("id_0", initiative.getInitiativeId())
                                                                .replaceAll("(userId_[^\"]+)", "$1_FAMILYMEMBER" + i)
                                                        )
                                                        .copyHeaders(message.getHeaders())
                                                        .build()
                                        ))
                ).toList();
    }

    private void storeInitiativeCountersInitialState() {
        Assertions.assertTrue(expectedOnboardingKoFamilies>0, "Call this method after payload build in order to set the residual equals to onboardingOk expected");

        InitiativeCounters counter = new InitiativeCounters();
        counter.setId("INITIATIVEID");
        counter.setInitiativeBudgetCents(CommonUtilities.euroToCents(publishedInitiatives.get(0).getGeneral().getBudget()));
        counter.setOnboarded((long)expectedOnboardingKoFamilies);
        counter.setReservedInitiativeBudgetCents(CommonUtilities.euroToCents(publishedInitiatives.get(0).getGeneral().getBeneficiaryBudget()) * expectedOnboardingKoFamilies);
        counter.setResidualInitiativeBudgetCents(counter.getInitiativeBudgetCents() - counter.getReservedInitiativeBudgetCents());
        initiativeCountersRepository.save(counter).block();
    }

    private void storeMockedFamilyMembers(String userId) {
        String membersMockedBaseId = userId;
        if (membersMockedBaseId.matches(".*_FAMILYMEMBER\\d+$")) {
            membersMockedBaseId = membersMockedBaseId.substring(0, membersMockedBaseId.indexOf("_FAMILYMEMBER"));
        }

        mongoTemplate.save(FamilyDataRetrieverServiceImpl.MockedFamily.builder()
                                .familyId("FAMILYID_" + membersMockedBaseId)
                                .memberIds(new HashSet<>(List.of(
                                        membersMockedBaseId + "_FAMILYMEMBER0",
                                        membersMockedBaseId + "_FAMILYMEMBER1",
                                        membersMockedBaseId + "_FAMILYMEMBER2"
                                )))
                                .build())
                .block();
    }

    private <T extends EvaluationDTO> void checkResponses(int expectedRequestsPerInitiative, List<ConsumerRecord<String, String>> payloadConsumed,Class<T> clazz) throws JsonProcessingException {
        assertInitiativePublishedMessagesCount(expectedRequestsPerInitiative, payloadConsumed, clazz);

        List<T> evaluations = new ArrayList<>(payloadConsumed.size());

        for (ConsumerRecord<String, String> p : payloadConsumed) {
            T payload = objectMapper.readValue(p.value(), clazz);
            evaluations.add(payload);

            String userId = payload.getUserId();

            payload.setUserId(userId.replaceAll("_FAMILYMEMBER\\d+",""));
            checkResponse(payload, useCases);
            payload.setUserId(userId);
            if(payload.getUserId().startsWith("NOFAMILY")){
                Assertions.assertNull(payload.getFamilyId(), "Evaluation has familyId evenif not expected: " + payload);
            } else {
                Assertions.assertNotNull(payload.getFamilyId(), "Evaluation has null familyId: " + payload);
            }
        }

        Set<String> familyIds = evaluations.stream().map(EvaluationDTO::getFamilyId).filter(Objects::nonNull).collect(Collectors.toSet());
        Assertions.assertEquals(expectedOnboardedFamilies, familyIds.size(), () -> "Unexpected families count: " + familyIds.stream().sorted().toList());


        Map<String, List<T>> families = evaluations.stream()
                .filter(ev->ev.getFamilyId()!=null)
                .collect(Collectors.groupingBy(ev->ev.getFamilyId() + "_" + ev.getInitiativeId())); // not simply grouping by familyId because ranking KO publish also in outcome

        if (clazz.equals(EvaluationCompletedDTO.class)) {
            families.values()
                    .forEach(members -> {
                        Map<OnboardingEvaluationStatus, Long> membersByStatusCount = members.stream().map(o -> (EvaluationCompletedDTO) o)
                                .collect(Collectors.groupingBy(EvaluationCompletedDTO::getStatus, Collectors.counting()));

                        Map<OnboardingEvaluationStatus, Long> expected;

                        if (membersByStatusCount.containsKey(OnboardingEvaluationStatus.ONBOARDING_OK)) {
                            expected = Map.of(
                                    OnboardingEvaluationStatus.ONBOARDING_OK, 1L,
                                    OnboardingEvaluationStatus.JOINED, (long) (membersPerFamily - 1),
                                    OnboardingEvaluationStatus.DEMANDED, (long) (membersPerFamily - 1)
                            );
                        } else {
                            expected = Map.of(
                                    OnboardingEvaluationStatus.ONBOARDING_KO, 1L,
                                    OnboardingEvaluationStatus.REJECTED, (long) (membersPerFamily - 1)
                            );
                        }

                        Assertions.assertEquals(expected, membersByStatusCount);
                    });
        }
    }

    private void assertInitiativePublishedMessagesCount(int expectedMessages, List<ConsumerRecord<String, String>> publishedRecords, Class<? extends EvaluationDTO> clazz) {
        Assertions.assertEquals(expectedMessages, publishedRecords.size(), ()-> {
            Set<Map.Entry<String, Long>> userId2MessagesCount = publishedRecords.stream()
                            .map(r -> {
                                try {
                                    return objectMapper.readValue(r.value(), clazz);
                                } catch (JsonProcessingException e) {
                                    throw new IllegalStateException(e);
                                }
                            })
                            .filter(r -> !(r instanceof EvaluationCompletedDTO completedDTO) || !OnboardingEvaluationStatus.DEMANDED.equals(completedDTO.getStatus()))
                            .collect(Collectors.groupingBy(ev -> ev.getUserId() + "_" + ev.getInitiativeId(), Collectors.counting()))
                            .entrySet();

                    return "Unexpected published message count, %s: duplicates: %s".formatted(
                            expectedMessages == userId2MessagesCount.size()
                                    ? "there are some duplicates"
                            : "distinct messages: " + userId2MessagesCount.size(),
                            userId2MessagesCount.stream()
                            .filter(e -> e.getValue() > 1)
                            .map(Map.Entry::getKey)
                            .toList());
                }
        );
    }

    private void checkStoredOnboardingFamilies() {
        Map<String, List<OnboardingFamilies>> initiative2onboardingFamilies = onboardingFamiliesRepository.findAll().collect(Collectors.groupingBy(OnboardingFamilies::getInitiativeId)).block();
        Assertions.assertNotNull(initiative2onboardingFamilies);

        publishedInitiatives.forEach(i-> {
            List<OnboardingFamilies> onboardingFamilies = initiative2onboardingFamilies.get(i.getInitiativeId());
            Assertions.assertNotNull(onboardingFamilies);

            Assertions.assertEquals(
                    expectedOnboardedFamilies,
                    onboardingFamilies.size(),
                    "Initiative %s has an unexpected onboarding families count".formatted(i.getInitiativeId())
            );

            Assertions.assertEquals(expectedOnboardingKoFamilies - expectedFamilyRetrieveKo,
                    onboardingFamilies.stream().filter(f->OnboardingFamilyEvaluationStatus.ONBOARDING_KO.equals(f.getStatus())).count());

            Assertions.assertTrue(onboardingFamilies.stream().allMatch(f->
                    OnboardingFamilyEvaluationStatus.ONBOARDING_OK.equals(f.getStatus()) ||
                            OnboardingFamilyEvaluationStatus.ONBOARDING_KO.equals(f.getStatus()))
            );
        });
    }

    private void checkInitiativeCounters() {
        { // Using anonymous block in order to be sure to not re-use variables
            InitiativeCounters counter = initiativeCountersRepository.findById("INITIATIVEID").block();
            Assertions.assertNotNull(counter);

            // this counter is initialized in order to compensate KO, thus at the end the budget should be exhausted
            Assertions.assertEquals(onboardingFamilies, counter.getOnboarded());
            Assertions.assertEquals(0L, counter.getResidualInitiativeBudgetCents());
        }

        { // Using anonymous block in order to be sure to not re-use variables
            InitiativeCounters rankingCounter = initiativeCountersRepository.findById("RANKINGINITIATIVEID").block();
            Assertions.assertNotNull(rankingCounter);

            Assertions.assertEquals(0L, rankingCounter.getOnboarded());
            Assertions.assertEquals(
                    CommonUtilities.euroToCents(publishedInitiatives.get(0).getGeneral().getBudget())
                    , rankingCounter.getResidualInitiativeBudgetCents());
        }
    }

    //region useCases

    private OnboardingDTO.OnboardingDTOBuilder buildOnboardingRequestBuilder(Integer bias) {
        return OnboardingDTOFaker.mockInstanceBuilder(bias, 1)
                .isee(BigDecimal.valueOf(20))
                .birthDate(new BirthDate("1990", LocalDate.now().getYear() - 1990));
    }

    // each useCase's userId should contain "userId_[0-9]+", this string is matched in order to set particular member id
    Set<OnboardingEvaluationStatus> expectedOnboardingOkStatuses = Set.of(OnboardingEvaluationStatus.ONBOARDING_OK, OnboardingEvaluationStatus.JOINED, OnboardingEvaluationStatus.DEMANDED);
    Set<OnboardingEvaluationStatus> expectedOnboardingKoStatuses = Set.of(OnboardingEvaluationStatus.ONBOARDING_KO, OnboardingEvaluationStatus.REJECTED);
    private final List<OnboardingUseCase<EvaluationDTO>> useCases = List.of(
            // useCase 0: onboardingOk
            OnboardingUseCase.withJustPayload(
                    bias -> {
                        OnboardingDTO request = buildOnboardingRequestBuilder(bias).build();
                        storeMockedFamilyMembers(request.getUserId());
                        return request;
                    },
                    evaluation -> {
                        if(evaluation instanceof RankingRequestDTO rankingRequest){
                            Assertions.assertFalse(rankingRequest.isOnboardingKo());
                        } else if(evaluation instanceof EvaluationCompletedDTO evaluationCompleted) {
                            Assertions.assertEquals(Collections.emptyList(), evaluationCompleted.getOnboardingRejectionReasons());
                            Assertions.assertTrue(expectedOnboardingOkStatuses.contains(evaluationCompleted.getStatus()), "Unexpected status: " + evaluationCompleted.getStatus());
                        }
                    }
            ),

            // useCase 1: onboardingKo case
            OnboardingUseCase.withJustPayload(
                    bias -> {
                        expectedOnboardingKoFamilies++;
                        OnboardingDTO request = buildOnboardingRequestBuilder(bias)
                                .isee(BigDecimal.ZERO)
                                .build();
                        storeMockedFamilyMembers(request.getUserId());
                        return request;
                    },
                    evaluation -> {
                        if(evaluation instanceof RankingRequestDTO rankingRequest){
                            Assertions.assertTrue(rankingRequest.isOnboardingKo());
                        } else if(evaluation instanceof EvaluationCompletedDTO evaluationCompleted) {
                            Assertions.assertTrue(expectedOnboardingKoStatuses.contains(evaluationCompleted.getStatus()));
                            Assertions.assertEquals(
                                    List.of(new OnboardingRejectionReason(
                                            OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL,
                                            OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted(CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE),
                                            CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE_AUTH,
                                            CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE_AUTH_LABEL,
                                            null
                                    )),
                                    evaluationCompleted.getOnboardingRejectionReasons());
                        }
                    }
            ),

            // useCase 2: onboardingKo case due to family not found
            OnboardingUseCase.withJustPayload(
                    bias -> {
                        expectedOnboardingKoFamilies++;
                        expectedFamilyRetrieveKo++;
                        OnboardingDTO request = buildOnboardingRequestBuilder(bias)
                                .userId("NOFAMILYuserId_" + bias)
                                .build();
                        Mockito.doReturn(Mono.just(Optional.empty()))
                                .when(familyDataRetrieverServiceSpy)
                                .retrieveFamily(Mockito.argThat(r->r.getUserId().startsWith(request.getUserId())), Mockito.any());
                        return request;
                    },
                    evaluation -> {
                        if(evaluation instanceof RankingRequestDTO rankingRequest){
                            Assertions.assertTrue(rankingRequest.isOnboardingKo());
                        } else if(evaluation instanceof EvaluationCompletedDTO evaluationCompleted) {
                            Assertions.assertTrue(expectedOnboardingKoStatuses.contains(evaluationCompleted.getStatus()));
                            Assertions.assertEquals(
                                    List.of(new OnboardingRejectionReason(
                                            OnboardingRejectionReason.OnboardingRejectionReasonType.FAMILY_KO,
                                            OnboardingConstants.REJECTION_REASON_FAMILY_KO,
                                            CriteriaCodeConfigFaker.CRITERIA_CODE_FAMILY_AUTH,
                                            CriteriaCodeConfigFaker.CRITERIA_CODE_FAMILY_AUTH_LABEL,
                                            "Nucleo familiare non disponibile"
                                    )),
                                    evaluationCompleted.getOnboardingRejectionReasons());
                        }
                    }
            )
    );
    //endregion


    protected void checkPayload(String errorMessage, String expectedPayload) {
        try {
            RankingRequestDTO actual = objectMapper.readValue(errorMessage, RankingRequestDTO.class);
            RankingRequestDTO expected = objectMapper.readValue(expectedPayload, RankingRequestDTO.class);

            TestUtils.checkNotNullFields(actual, "familyId");
            Assertions.assertEquals(expected.getUserId(), actual.getUserId());
            Assertions.assertEquals(expected.getInitiativeId(), actual.getInitiativeId());
        } catch (JsonProcessingException e) {
            Assertions.fail("Error check in payload");
        }
    }
}