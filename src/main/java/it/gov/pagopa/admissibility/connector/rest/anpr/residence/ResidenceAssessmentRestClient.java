package it.gov.pagopa.admissibility.connector.rest.anpr.residence;

import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import reactor.core.publisher.Mono;

public interface ResidenceAssessmentRestClient {
    Mono<RispostaE002OKDTO> getResidenceAssessment(String accessToken, String fiscalCode, AgidJwtTokenPayload agidJwtTokenPayload);
}