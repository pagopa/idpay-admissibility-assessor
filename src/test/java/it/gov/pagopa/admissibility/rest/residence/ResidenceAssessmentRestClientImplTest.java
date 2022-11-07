package it.gov.pagopa.admissibility.rest.residence;

import it.gov.pagopa.admissibility.BaseIntegrationTest;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ResidenceAssessmentRestClientImplTest extends BaseIntegrationTest {

    @Autowired
    private ResidenceAssessmentRestClient residenceAssessmentRestClient;

    @Test
    void getResidenceAssessment(){
        RispostaE002OKDTO result = residenceAssessmentRestClient.getResidenceAssessment("ACCESS_TOKEN", "FISCAL_CODE").block();

        Assertions.assertNotNull(result);
    }

}