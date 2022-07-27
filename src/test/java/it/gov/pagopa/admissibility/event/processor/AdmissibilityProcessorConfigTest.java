package it.gov.pagopa.admissibility.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.dto.build.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import it.gov.pagopa.admissibility.test.fakers.OnboardingDTOFaker;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class AdmissibilityProcessorConfigTest extends BaseIntegrationTest {

    @Test
    void testAdmissibilityOnboarding() throws JsonProcessingException {
        int I = 5;
        int N=10;

        int[] expectedRules ={0};
        List<Initiative2BuildDTO> initiatives = IntStream.range(0,I)
                .mapToObj(Initiative2BuildDTOFaker::mockInstance)
                .toList();

        initiatives.forEach(i->publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, null, null, i));
        wait(10000); //TODO

        List<OnboardingDTO> onboardings = IntStream.range(0, N)
                .mapToObj(n -> OnboardingDTOFaker.mockInstance(n,I)).toList();

        long timeStart=System.currentTimeMillis();
        onboardings.forEach(i->publishIntoEmbeddedKafka(topicAdmissibilityProcessorRequest, null, null, i));
        long timePublishingEnd=System.currentTimeMillis();
        wait(10000);

        Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topicAdmissibilityProcessorOutcome, "idpay-group");

        int i = 0;
        int counter = 0;
        while (i < N) {
            ConsumerRecords<String, String> published = consumer.poll(Duration.ofMillis(7000));
            for (ConsumerRecord<String, String> record : published) {
                EvaluationDTO evaluation = objectMapper.readValue(record.value(),EvaluationDTO.class);
                String initiativeId = evaluation.getInitiativeId();
                Integer biasRetrieve = Integer.parseInt(initiativeId.substring(3, initiatives.size()-1));

                if(biasRetrieve%I==0){
                    Assertions.assertNotNull(evaluation.getOnboardingRejectionReasons());
                    Assertions.assertFalse(evaluation.getOnboardingRejectionReasons().isEmpty());
                    Assertions.assertTrue(evaluation.getOnboardingRejectionReasons().contains("CONSENSUS_CHECK_SELF_DECLARATION_BIRTHDATE_FAIL"));
                }else {
                    Assertions.assertTrue(evaluation.getOnboardingRejectionReasons().isEmpty());
                }
                log.info(evaluation.toString());
                counter++;

                i++;
            }
        }
        Assertions.assertEquals(N,counter);

    }
}