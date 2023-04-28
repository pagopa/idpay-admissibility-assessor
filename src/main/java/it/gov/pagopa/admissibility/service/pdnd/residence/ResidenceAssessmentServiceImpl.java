package it.gov.pagopa.admissibility.service.pdnd.residence;

import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.connector.rest.anpr.residence.ResidenceAssessmentRestClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ResidenceAssessmentServiceImpl implements ResidenceAssessmentService{
    private final ResidenceAssessmentRestClient residenceAssessmentRestClient;

    public ResidenceAssessmentServiceImpl(ResidenceAssessmentRestClient residenceAssessmentRestClient) {
        this.residenceAssessmentRestClient = residenceAssessmentRestClient;
    }

    @Override
    public Mono<RispostaE002OKDTO> getResidenceAssessment(String accessToken, String fiscalCode, AgidJwtTokenPayload agidJwtTokenPayload) {
        return residenceAssessmentRestClient.getResidenceAssessment(accessToken, fiscalCode, agidJwtTokenPayload);
    }
}