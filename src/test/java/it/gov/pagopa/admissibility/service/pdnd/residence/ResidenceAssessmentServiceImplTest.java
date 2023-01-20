package it.gov.pagopa.admissibility.service.pdnd.residence;

import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.rest.anpr.residence.ResidenceAssessmentRestClient;
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

        AgidJwtTokenPayload tokenPayload = AgidJwtTokenPayload.builder()
                .iss("ISS")
                .sub("SUB")
                .aud("AUD").build();

        RispostaE002OKDTO rispostaE002OKDTOMock = Mockito.mock(RispostaE002OKDTO.class);
        Mockito.when(residenceAssessmentRestClientMock.getResidenceAssessment(accessToken, fiscalCode, tokenPayload)).thenReturn(Mono.just(rispostaE002OKDTOMock));

        // When
        RispostaE002OKDTO result = residenceAssessmentService.getResidenceAssessment(accessToken, fiscalCode, tokenPayload).block();

        // Then
        Assertions.assertNotNull(result);
        Mockito.verify(residenceAssessmentRestClientMock).getResidenceAssessment(Mockito.anyString(), Mockito.anyString(), Mockito.any(AgidJwtTokenPayload.class));
    }
}