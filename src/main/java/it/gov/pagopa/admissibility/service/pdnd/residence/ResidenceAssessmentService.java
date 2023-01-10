package it.gov.pagopa.admissibility.service.pdnd.residence;

import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import reactor.core.publisher.Mono;

public interface ResidenceAssessmentService {
    Mono<RispostaE002OKDTO> getResidenceAssessment(String accessToken, String fiscalCode);
}