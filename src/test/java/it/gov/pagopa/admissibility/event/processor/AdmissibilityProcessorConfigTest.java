package it.gov.pagopa.admissibility.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.event.consumer.BeneficiaryRuleConsumerConfigIntegrationTest;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

@Slf4j
@TestPropertySource(properties = {
        "app.beneficiary-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.admissibility.service.build.BeneficiaryRule2DroolsRuleImpl=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.build.KieContainerBuilderServiceImpl=WARN",
        "logging.level.it.gov.pagopa.admissibility.service.onboarding.AdmissibilityMediatorServiceImpl=WARN",
})
class AdmissibilityProcessorConfigTest extends BaseIntegrationTest {
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
        IntStream.range(0, initiativesNumber)
                .mapToObj(Initiative2BuildDTOFaker::mockInstance)
                .peek(i->expectedRules[0]+=i.getBeneficiaryRule().getAutomatedCriteria().size())
                .forEach(i->publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, null, null, i));

       BeneficiaryRuleConsumerConfigIntegrationTest.waitForKieContainerBuild(expectedRules[0],onboardingContextHolderServiceSpy);
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
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstance(bias, initiativesNumber),
                    evaluation -> {
                        Assertions.assertTrue(evaluation.getOnboardingRejectionReasons().isEmpty());
                        Assertions.assertEquals("ONBOARDING_OK", evaluation.getStatus());
                    }
            ),
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceTcFalse(bias, initiativesNumber),
                    evaluation -> checkKO(evaluation.getStatus(), evaluation.getOnboardingRejectionReasons(), "CONSENSUS_CHECK_TC_FAIL")
            ),
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstancePdndFalse(bias, initiativesNumber),
                    evaluation -> checkKO(evaluation.getStatus(), evaluation.getOnboardingRejectionReasons(), "CONSENSUS_CHECK_PDND_FAIL")
            ),
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceIseeDeclarationFalse(bias, initiativesNumber),
                    evaluation -> checkKO(evaluation.getStatus(), evaluation.getOnboardingRejectionReasons(), "CONSENSUS_CHECK_SELF_DECLARATION_ISEE_FAIL")
            ),
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceBirthdateDeclarationFalse(bias, initiativesNumber),
                    evaluation -> checkKO(evaluation.getStatus(), evaluation.getOnboardingRejectionReasons(), "CONSENSUS_CHECK_SELF_DECLARATION_BIRTHDATE_FAIL")
            ),
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceTcAcceptTimestampNotValid(bias, initiativesNumber),
                    evaluation -> checkKO(evaluation.getStatus(), evaluation.getOnboardingRejectionReasons(), "CONSENSUS_CHECK_TC_ACCEPT_FAIL")
            ),
            Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceCriteriaConsensusTimestampNotValid(bias, initiativesNumber),
                    evaluation -> checkKO(evaluation.getStatus(), evaluation.getOnboardingRejectionReasons(), "CONSENSUS_CHECK_CRITERIA_CONSENSUS_FAIL")

            ), Pair.of(
                    bias -> OnboardingDTOFaker.mockInstanceIseeNotValid(bias, initiativesNumber),
                    evaluation -> checkKO(evaluation.getStatus(), evaluation.getOnboardingRejectionReasons(),"AUTOMATED_CRITERIA_ISEE_FAIL")
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