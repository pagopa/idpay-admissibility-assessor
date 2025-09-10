package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.ConsultazioneSogliaIndicatoreResponseType;
import reactor.core.publisher.Mono;

public interface IseeThresholdConsultationSoapClient {
    Mono<ConsultazioneSogliaIndicatoreResponseType> verifyThresholdIsee(String fiscalCode, String thresholdCode);
}
