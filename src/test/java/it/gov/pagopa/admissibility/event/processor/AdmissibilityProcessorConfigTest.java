package it.gov.pagopa.admissibility.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.event.consumer.BeneficiaryRuleBuilderConsumerConfigIntegrationTest;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

@Slf4j
@TestPropertySource(properties = {
        "app.beneficiary-rule.build-delay-duration=PT1S",
        "app.beneficiary-rule.cache.refresh-ms-rate:60000",
        "logging.level.it.gov.pagopa.admissibility.service.build.BeneficiaryRule2DroolsRuleImpl=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.AdmissibilityEvaluatorMediatorServiceImpl=WARN",
})
class AdmissibilityProcessorConfigTest extends BaseIntegrationTest {
    public static final String EXHAUSTED_INITIATIVE_ID = "EXHAUSTED_INITIATIVE_ID";

    @SpyBean
    private OnboardingContextHolderService onboardingContextHolderServiceSpy;
    private final int initiativesNumber = 5;

    @Test
    void testAdmissibilityOnboarding() throws JsonProcessingException {
        int onboardingsNumber = 1000;
        long maxWaitingMs=30000;

        publishOnboardingRules();

        List<OnboardingDTO> onboardings = IntStream.range(0, onboardingsNumber)
                .mapToObj(this::mockInstance).toList();

        long timePublishOnboardingStart=System.currentTimeMillis();
        onboardings.forEach(i->publishIntoEmbeddedKafka(topicAdmissibilityProcessorRequest, null, null, i));
        long timePublishingOnboardingRequest=System.currentTimeMillis()-timePublishOnboardingStart;

        Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topicAdmissibilityProcessorOutcome, "idpay-group");

        long timeConsumerResponse=System.currentTimeMillis();

        List<String> payloadConsumed = new ArrayList<>(onboardingsNumber);
        int counter = 0;
        while(counter<onboardingsNumber) {
            if(System.currentTimeMillis()-timeConsumerResponse>maxWaitingMs){
                Assertions.fail("timeout of %d ms expired".formatted(maxWaitingMs));
            }
            ConsumerRecords<String, String> published = consumer.poll(Duration.ofMillis(7000));
            for (ConsumerRecord<String, String> record : published) {
                payloadConsumed.add(record.value());
                counter++;
            }
        }
        long timeEnd=System.currentTimeMillis();
        long timeConsumerResponseEnd = timeEnd-timeConsumerResponse;
        Assertions.assertEquals(onboardingsNumber,counter);
        for (String p : payloadConsumed) {
            EvaluationDTO evaluation = objectMapper.readValue(p, EvaluationDTO.class);
            checkResponse(evaluation);
        }

        System.out.printf("""
            ************************
            Time spent to send %d onboarding request messages: %d millis
            Time spent to consume onboarding responses: %d millis
            ************************
            Test Completed in %d millis
            ************************
            """,
                onboardingsNumber, timePublishingOnboardingRequest,
                timeConsumerResponseEnd,
                timeEnd-timePublishOnboardingStart
        );
    }

    private void publishOnboardingRules() {
        int[] expectedRules ={0};
        IntStream.range(0, initiativesNumber+1)
                .mapToObj(Initiative2BuildDTOFaker::mockInstance)
                .peek(i->expectedRules[0]+=i.getBeneficiaryRule().getAutomatedCriteria().size())
                .peek(i->{
                    if(i.getInitiativeId().endsWith("_"+initiativesNumber)){
                        i.setInitiativeId(EXHAUSTED_INITIATIVE_ID);
                        i.getGeneral().setBudget(BigDecimal.ZERO);
                    }
                })
                .forEach(i-> publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, null, null, i));


       BeneficiaryRuleBuilderConsumerConfigIntegrationTest.waitForKieContainerBuild(expectedRules[0],onboardingContextHolderServiceSpy);
    }

    OnboardingDTO mockInstance(int bias){
        return useCases.get(bias% useCases.size()).getFirst().apply(bias);
    }

    void checkResponse(EvaluationDTO evaluation){
        String userId = evaluation.getUserId();
        int biasRetrieve = Integer.parseInt(userId.substring(7));
        useCases.get(biasRetrieve% useCases.size()).getSecond().accept(evaluation);

    }

    //region useCases
    private final List<Pair<Function<Integer,OnboardingDTO>, java.util.function.Consumer<EvaluationDTO>>> useCases= List.of(
            //successful case
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstance(bias, initiativesNumber),
                    evaluation -> {
                        Assertions.assertTrue(evaluation.getOnboardingRejectionReasons().isEmpty());
                        Assertions.assertEquals("ONBOARDING_OK", evaluation.getStatus());
                    }
            ),
            // TC consensus fail
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .tc(false)
                            .build(),
                    evaluation -> checkKO(evaluation.getStatus(), evaluation.getOnboardingRejectionReasons(), OnboardingConstants.REJECTION_REASON_CONSENSUS_TC_FAIL)
            ),
            // PDND consensuns fail
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .pdndAccept(false)
                            .build(),
                    evaluation -> checkKO(evaluation.getStatus(), evaluation.getOnboardingRejectionReasons(), OnboardingConstants.REJECTION_REASON_CONSENSUS_PDND_FAIL)
            ),
            // self declaration fail
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .selfDeclarationList(Map.of("DUMMY", false))
                            .build(),
                    evaluation -> checkKO(evaluation.getStatus(), evaluation.getOnboardingRejectionReasons(), OnboardingConstants.REJECTION_REASON_CONSENSUS_CHECK_SELF_DECLARATION_FAIL_FORMAT.formatted("DUMMY"))
            ),
            // TC acceptance timestamp fail
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .tcAcceptTimestamp(LocalDateTime.now().withYear(1970))
                            .build(),
                    evaluation -> checkKO(evaluation.getStatus(), evaluation.getOnboardingRejectionReasons(), OnboardingConstants.REJECTION_REASON_TC_CONSENSUS_DATETIME_FAIL)
            ),
            // TC criteria acceptance timestamp fail
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .criteriaConsensusTimestamp(LocalDateTime.now().withYear(1970))
                            .build(),
                    evaluation -> checkKO(evaluation.getStatus(), evaluation.getOnboardingRejectionReasons(), OnboardingConstants.REJECTION_REASON_CRITERIA_CONSENSUS_DATETIME_FAIL)

            ),
            // AUTOMATED_CRITERIA fail
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .isee(BigDecimal.TEN)
                            .build(),
                    evaluation -> checkKO(evaluation.getStatus(), evaluation.getOnboardingRejectionReasons(),OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted("ISEE"))
            ),
            // exhausted initiative budget
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBuilder(bias, initiativesNumber)
                            .initiativeId(EXHAUSTED_INITIATIVE_ID)
                            .build(),
                    evaluation -> checkKO(evaluation.getStatus(), evaluation.getOnboardingRejectionReasons(), OnboardingConstants.REJECTION_REASON_INITIATIVE_BUDGET_EXHAUSTED)
            )
    );
    //endregion

    void checkKO(String status, List<String> evaluationRejectionReason, String expected){
        Assertions.assertEquals("ONBOARDING_KO", status);
        Assertions.assertNotNull(evaluationRejectionReason);
        Assertions.assertTrue(evaluationRejectionReason.contains(expected),
                "Expected rejection reason %s and obtained %s".formatted(expected, evaluationRejectionReason));
    }

}