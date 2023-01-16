package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface AnprInvocationService {
    Mono<Optional<RispostaE002OKDTO>> invoke(String accessToken, String fiscalCode);

    void extract(RispostaE002OKDTO anprResponse, boolean getResidence, boolean getBirthDate, OnboardingDTO onboardingRequest);
}
