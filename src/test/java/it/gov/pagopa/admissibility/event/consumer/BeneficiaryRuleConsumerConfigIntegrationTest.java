package it.gov.pagopa.admissibility.event.consumer;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BeneficiaryRuleConsumerConfigIntegrationTest extends BaseIntegrationTest {

    @Test
    public void testBeneficiaryRuleBuilding(){
        publishIntoEmbeddedKafka(topicBeneficiaryRuleConsumer, null, null, Initiative2BuildDTOFaker.mockInstance(0));
        wait(2000);
        Assertions.assertEquals(1, droolsRuleRepository.count().block());
        // TODO extend test
    }

}
