package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface InpsInvocationService {

    Mono<Optional<ConsultazioneIndicatoreResponseType>> invoke(String fiscalCode, IseeTypologyEnum iseeType);

    void extract(ConsultazioneIndicatoreResponseType inpsResponse, boolean getIsee, OnboardingDTO onboardingRequest);
}
