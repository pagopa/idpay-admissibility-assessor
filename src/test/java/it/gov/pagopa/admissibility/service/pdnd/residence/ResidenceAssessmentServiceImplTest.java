package it.gov.pagopa.admissibility.service.pdnd.residence;

import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.rest.residence.ResidenceAssessmentRestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class ResidenceAssessmentServiceImplTest {

    @Test
    void getResidenceAssessment() {
        // Given
        ResidenceAssessmentRestClient residenceAssessmentRestClientMock = Mockito.mock(ResidenceAssessmentRestClient.class);
        ResidenceAssessmentService residenceAssessmentService = new ResidenceAssessmentServiceImpl(residenceAssessmentRestClientMock);
        String accessToken = "ACCESS_TOKEN";
        String fiscalCode = "FISCAL_CODE";

        RispostaE002OKDTO rispostaE002OKDTOMock = Mockito.mock(RispostaE002OKDTO.class);
        Mockito.when(residenceAssessmentRestClientMock.getResidenceAssessment(accessToken, fiscalCode)).thenReturn(Mono.just(rispostaE002OKDTOMock));

        // When
        RispostaE002OKDTO result = residenceAssessmentService.getResidenceAssessment(accessToken, fiscalCode).block();

        // Then
        Assertions.assertNotNull(result);
        Mockito.verify(residenceAssessmentRestClientMock).getResidenceAssessment(Mockito.anyString(), Mockito.anyString());
    }
}