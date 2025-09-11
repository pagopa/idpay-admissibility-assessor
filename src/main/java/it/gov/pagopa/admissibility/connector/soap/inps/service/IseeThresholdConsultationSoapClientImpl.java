package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.connector.soap.inps.config.InpsThresholdClientConfig;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.*;
import it.gov.pagopa.common.reactive.soap.utils.SoapUtils;
import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.xml.datatype.DatatypeFactory;
import java.util.GregorianCalendar;
import java.util.Set;

@Service
@Slf4j
public class IseeThresholdConsultationSoapClientImpl implements IseeThresholdConsultationSoapClient {
    private static final Set<EsitoEnum> RETRYABLE_OUTCOMES = Set.of(
            EsitoEnum.DATABASE_OFFLINE, EsitoEnum.ERRORE_INTERNO
    );

    private static final DatatypeFactory DATATYPE_FACTORY = DatatypeFactory.newDefaultInstance();

    private final ISvcConsultazione portSvcConsultazione;

    public IseeThresholdConsultationSoapClientImpl(InpsThresholdClientConfig inpsClientConfig) {

        this.portSvcConsultazione = inpsClientConfig.getPortSvcConsultazione();
    }


    private ConsultazioneSogliaIndicatoreRequestType getRequest(String fiscalCode, String thresholdCode) {
        ConsultazioneSogliaIndicatoreRequestType consultazioneSogliaIndicatoreRequestType = new ConsultazioneSogliaIndicatoreRequestType();

        consultazioneSogliaIndicatoreRequestType.setCodiceFiscale(fiscalCode);
        consultazioneSogliaIndicatoreRequestType.setCodiceSoglia(thresholdCode);
        consultazioneSogliaIndicatoreRequestType.setFornituraNucleo(SiNoEnum.SI);
        consultazioneSogliaIndicatoreRequestType.setDataValidita(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));

        return consultazioneSogliaIndicatoreRequestType;
    }

    private Mono<ConsultazioneSogliaIndicatoreResponse> callService(String fiscalCode, String thresholdCode) {
        return SoapUtils.soapInvoke2Mono(asyncHandler ->
                portSvcConsultazione.consultazioneSogliaIndicatoreAsync(getRequest(fiscalCode, thresholdCode), asyncHandler));
    }

    @Override
    public Mono<ConsultazioneSogliaIndicatoreResponseType> verifyThresholdIsee(String fiscalCode, String thresholdCode) {
        return PerformanceLogger.logTimingOnNext(
                "INPS_INVOCATION",
                callService(fiscalCode, thresholdCode)
                        .map(ConsultazioneSogliaIndicatoreResponse::getConsultazioneSogliaIndicatoreResult)
                        .flatMap(result -> IseeUtils.handleServiceOutcome(
                                result,
                                result.getEsito(),
                                result.getIdRichiesta(),
                                result.getDescrizioneErrore(),
                                RETRYABLE_OUTCOMES,
                                EsitoEnum.OK
                        ))
                        .onErrorResume(IseeUtils::handleError),
                x -> "THRESHOLD_ISEE");
    }
}
