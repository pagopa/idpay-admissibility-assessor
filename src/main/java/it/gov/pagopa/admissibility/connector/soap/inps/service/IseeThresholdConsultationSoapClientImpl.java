package it.gov.pagopa.admissibility.connector.soap.inps.service;

import com.sun.xml.ws.client.ClientTransportException;
import it.gov.pagopa.admissibility.connector.soap.inps.config.InpsThresholdClientConfig;
import it.gov.pagopa.admissibility.connector.soap.inps.exception.InpsDailyRequestLimitException;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.*;
import it.gov.pagopa.common.reactive.soap.utils.SoapUtils;
import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.xml.datatype.DatatypeFactory;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class IseeThresholdConsultationSoapClientImpl implements IseeThresholdConsultationSoapClient {
    private static final Set<EsitoEnum> RETRYABLE_OUTCOMES = Set.of(
            EsitoEnum.DATABASE_OFFLINE, EsitoEnum.ERRORE_INTERNO
    );

    private final ISvcConsultazione portSvcConsultazione;

    public IseeThresholdConsultationSoapClientImpl(InpsThresholdClientConfig inpsClientConfig) {

        this.portSvcConsultazione = inpsClientConfig.getPortSvcConsultazione();
    }


    private ConsultazioneSogliaIndicatoreRequestType getRequest(String fiscalCode, String thresholdCode) {
        ConsultazioneSogliaIndicatoreRequestType consultazioneSogliaIndicatoreRequestType = new ConsultazioneSogliaIndicatoreRequestType();

        consultazioneSogliaIndicatoreRequestType.setCodiceFiscale(fiscalCode);
        consultazioneSogliaIndicatoreRequestType.setCodiceSoglia(thresholdCode);
        consultazioneSogliaIndicatoreRequestType.setFornituraNucleo(SiNoEnum.SI);
        consultazioneSogliaIndicatoreRequestType.setDataValidita(DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(new GregorianCalendar()));  //TODO TBV field not required

        return consultazioneSogliaIndicatoreRequestType;
    }

    private Mono<ConsultazioneSogliaIndicatoreResponse> callService(String fiscalCode, String thresholdCode) {
        return SoapUtils.soapInvoke2Mono(asyncHandler ->
                portSvcConsultazione.consultazioneSogliaIndicatoreAsync(getRequest(fiscalCode, thresholdCode), asyncHandler));
    }

    @Override
    public Mono<ConsultazioneSogliaIndicatoreResponseType> verifyThresholdIsee(String fiscalCode, String thresholdCode) {
        // TODO why pdnd's accessToken is not used?
        return PerformanceLogger.logTimingOnNext(
                "INPS_INVOCATION",
                callService(fiscalCode, thresholdCode)
                        .flatMap(response -> {
                            ConsultazioneSogliaIndicatoreResponseType result = response.getConsultazioneSogliaIndicatoreResult();
                            if (RETRYABLE_OUTCOMES.contains(result.getEsito())) {
                                log.warn("[ONBOARDING_REQUEST][INPS_INVOCATION] Invocation returned a retryable result! {} - {}: {}", result.getIdRichiesta(), result.getEsito(), result.getDescrizioneErrore());
                                return Mono.empty(); // Returning empty in order to retry later
                            } else {
                                if(!EsitoEnum.OK.equals(result.getEsito())){
                                    log.error("[ONBOARDING_REQUEST][INPS_INVOCATION] Invocation returned no data!  esito:{} id:{}: {}", result.getEsito(), result.getIdRichiesta(), result.getDescrizioneErrore());
                                }
                                return Mono.just(result);
                            }
                        })

                        //TODO define error code for retry
                        .onErrorResume(ExecutionException.class, e -> {
                            if (e.getCause() instanceof ClientTransportException clientTransportException && clientTransportException.getMessage().contains("Too Many Requests")) {
                                return Mono.error(new InpsDailyRequestLimitException(e));
                            } else {
                                return Mono.error(new IllegalStateException("[ONBOARDING_REQUEST][INPS_INVOCATION] Something went wrong when invoking INPS service", e));
                            }
                        })
                , x -> "THRESHOLD_ISEE");

    }

}
