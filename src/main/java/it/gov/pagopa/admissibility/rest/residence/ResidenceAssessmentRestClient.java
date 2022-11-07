package it.gov.pagopa.admissibility.rest.residence;

import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import reactor.core.publisher.Mono;

public interface ResidenceAssessmentRestClient {
    Mono<RispostaE002OKDTO> getResidenceAssessment(String accessToken, String fiscalCode);  //TODO check responseType
}