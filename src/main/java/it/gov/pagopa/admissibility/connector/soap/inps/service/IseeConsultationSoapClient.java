package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import reactor.core.publisher.Mono;

public interface IseeConsultationSoapClient {
    Mono<ConsultazioneIndicatoreResponseType> getIsee(String fiscalCode, IseeTypologyEnum iseeType);
}
