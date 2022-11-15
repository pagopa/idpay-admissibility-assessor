package it.gov.pagopa.admissibility.soap.inps;

import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponse;
import reactor.core.publisher.Mono;

public interface IseeConsultationSoapClient {
    Mono<ConsultazioneIndicatoreResponse> callService(String fiscalCode);
}
