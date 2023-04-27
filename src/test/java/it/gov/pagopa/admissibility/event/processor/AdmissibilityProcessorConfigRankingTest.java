package it.gov.pagopa.admissibility.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.RankingRequestDTO;
import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.event.consumer.BeneficiaryRuleBuilderConsumerConfigIntegrationTest;
import it.gov.pagopa.admissibility.service.onboarding.RankingNotifierService;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;


class AdmissibilityProcessorConfigRankingTest extends BaseAdmissibilityProcessorConfigTest {
    @SpyBean
    private RankingNotifierService rankingNotifierServiceSpy;

    private final int initiativesNumber = 3;

    @TestConfiguration
    static class MediatorSpyConfiguration extends BaseAdmissibilityProcessorConfigTest.MediatorSpyConfiguration {}

    @Test
    void testRankingAdmissibilityOnboarding() throws IOException {
        int validOnboardings = 100; // use even number
        int notValidOnboarding = errorUseCases.size();
        long maxWaitingMs = 30000;

        publishOnboardingRules(validOnboardings);

        List<String> onboardings = new ArrayList<>(buildValidPayloads(errorUseCases.size(), validOnboardings / 2, useCases));
        onboardings.addAll(IntStream.range(0, notValidOnboarding).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        onboardings.addAll(buildValidPayloads(errorUseCases.size() + (validOnboardings / 2) + notValidOnboarding, validOnboardings / 2, useCases));

        long timePublishOnboardingStart = System.currentTimeMillis();
        onboardings.forEach(i -> publishIntoEmbeddedKafka(topicAdmissibilityProcessorRequest, null, null, i));
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeConsumerResponse = System.currentTimeMillis();
        List<ConsumerRecord<String, String>> payloadConsumed = consumeMessages(topicAdmissibilityProcessorOutRankingRequest, validOnboardings, maxWaitingMs);
        long timeEnd = System.currentTimeMillis();

        long timeConsumerResponseEnd = timeEnd - timeConsumerResponse;
        Assertions.assertEquals(validOnboardings, payloadConsumed.size());

        for (ConsumerRecord<String, String> p : payloadConsumed) {
            RankingRequestDTO rankingRequest = objectMapper.readValue(p.value(), RankingRequestDTO.class);
            checkResponse(rankingRequest, useCases);
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

        checkOffsets(onboardings.size(), validOnboardings, topicAdmissibilityProcessorOutRankingRequest);
    }

    private void publishOnboardingRules(int onboardingsNumber) {
        int[] expectedRules = {0};
        IntStream.range(0, initiativesNumber)
                .mapToObj(i -> {
                    final Initiative2BuildDTO initiative = Initiative2BuildDTOFaker.mockInstanceBuilder(i)
                            .build();

                    initiative.getGeneral().setRankingEnabled(true);
                    initiative.getBeneficiaryRule().getAutomatedCriteria().get(0).setOrderDirection(Sort.Direction.ASC);

                    BigDecimal budget = initiative.getGeneral().getBeneficiaryBudget().multiply(BigDecimal.valueOf(onboardingsNumber));

                    initiative.getGeneral().setBudget(budget);

                    return initiative;
                })
                .peek(i -> expectedRules[0] += i.getBeneficiaryRule().getAutomatedCriteria().size())
                .forEach(i -> publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, null, null, i));

        BeneficiaryRuleBuilderConsumerConfigIntegrationTest.waitForKieContainerBuild(expectedRules[0], onboardingContextHolderServiceSpy);
    }

    //region useCases
    private final List<Pair<Function<Integer, OnboardingDTO>, Consumer<RankingRequestDTO>>> useCases = List.of(
            //successful case - coda ranking
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstance(bias, initiativesNumber),
                    rankingRequest -> {
                        Assertions.assertNotNull(rankingRequest.getUserId());
                        Assertions.assertNotNull(rankingRequest.getInitiativeId());
                        Assertions.assertNotNull(rankingRequest.getAdmissibilityCheckDate());
                        Assertions.assertNotNull(rankingRequest.getRankingValue());
                        Assertions.assertFalse(rankingRequest.isOnboardingKo());
                        Assertions.assertNull(rankingRequest.getFamilyId());
                    }
            ),

            //onboardingKo case
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .isee(BigDecimal.ZERO)
                            .build(),
                    rankingRequest -> {
                        Assertions.assertNotNull(rankingRequest.getUserId());
                        Assertions.assertNotNull(rankingRequest.getInitiativeId());
                        Assertions.assertNotNull(rankingRequest.getAdmissibilityCheckDate());
                        Assertions.assertNotNull(rankingRequest.getRankingValue());
                        Assertions.assertTrue(rankingRequest.isOnboardingKo());
                    }
            )
    );
    //endregion


    //region not valid useCases
    // all use cases configured must have a unique id recognized by the regexp errorUseCaseIdPatternMatch
    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();
    {
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
        return RankingRequestDTO.builder()
                .userId(onboardingDTO.getUserId())
                .initiativeId(onboardingDTO.getInitiativeId())
                .build();
    }
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