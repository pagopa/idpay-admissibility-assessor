package it.gov.pagopa.admissibility.soap.inps;

import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import reactor.core.publisher.Mono;

public interface IseeConsultationSoapClient {
    Mono<ConsultazioneIndicatoreResponseType> getIsee(String fiscalCode);
}
