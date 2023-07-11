package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import reactor.core.publisher.Mono;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

public interface IseeDataRetrieverService {

    Mono<Optional<ConsultazioneIndicatoreResponseType>> invoke(String fiscalCode, IseeTypologyEnum iseeType);

    @NotNull List<OnboardingRejectionReason> extract(ConsultazioneIndicatoreResponseType inpsResponse, boolean getIsee, OnboardingDTO onboardingRequest);
}
