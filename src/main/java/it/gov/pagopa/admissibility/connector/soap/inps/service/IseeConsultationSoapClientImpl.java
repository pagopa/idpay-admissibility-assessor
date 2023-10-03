package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.connector.soap.inps.config.InpsClientConfig;
import it.gov.pagopa.admissibility.connector.soap.inps.exception.InpsDailyRequestLimitException;
import it.gov.pagopa.admissibility.generated.soap.ws.client.*;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.common.reactive.soap.utils.SoapUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@Slf4j
public class IseeConsultationSoapClientImpl implements IseeConsultationSoapClient {
    private static final Set<EsitoEnum> RETRYABLE_OUTCOMES = Set.of(
            EsitoEnum.DATABASE_OFFLINE, EsitoEnum.ERRORE_INTERNO
    );

    private final ISvcConsultazione portSvcConsultazione;
    private final String requestProtocolEnte;
    private final String serviceToBeProvided;
    private final String requestStateEnte;

    public IseeConsultationSoapClientImpl(InpsClientConfig inpsClientConfig,
                                          @Value("${app.inps.request.request-protocol-ente}") String requestProtocolEnte,
                                          @Value("${app.inps.request.service-to-be-provided}")String serviceToBeProvided,
                                          @Value("${app.inps.request.request-state}")String requestStateEnte){

        this.requestProtocolEnte = requestProtocolEnte;
        this.serviceToBeProvided = serviceToBeProvided;
        this.requestStateEnte = requestStateEnte;
        this.portSvcConsultazione = inpsClientConfig.getPortSvcConsultazione();
    }

    @Override
    public Mono<ConsultazioneIndicatoreResponseType> getIsee(String fiscalCode, IseeTypologyEnum iseeType) {
        // TODO why pdnd's accessToken is not used?
        return callService(fiscalCode, iseeType)
                .flatMap(response -> {
                    ConsultazioneIndicatoreResponseType result = response.getConsultazioneIndicatoreResult();
                    if (RETRYABLE_OUTCOMES.contains(result.getEsito())) {
                        log.warn("[ONBOARDING_REQUEST][INPS_INVOCATION] Invocation returned a retryable result! {} - {}: {}; {}", result.getIdRichiesta(), result.getEsito(), result.getDescrizioneErrore(), result);
                        return Mono.empty(); // Returning empty in order to retry later
                    } else {
                        if(result.getEsito().equals(EsitoEnum.RICHIESTA_INVALIDA)){
                            log.error("[ONBOARDING_REQUEST][INPS_INVOCATION] Invocation returned invalid request!  {} - {}: {}; {}", result.getIdRichiesta(), result.getEsito(), result.getDescrizioneErrore(), result);
                        }
                        return Mono.just(result);
                    }
                })

                //TODO define error code for retry
                .doOnError(WebClientResponseException.TooManyRequests.class, e -> {
                    throw new InpsDailyRequestLimitException(e);
                });

    }

    private Mono<ConsultazioneIndicatoreResponse> callService(String fiscalCode, IseeTypologyEnum iseeType) {
        return SoapUtils.soapInvoke2Mono(asyncHandler ->
                portSvcConsultazione.consultazioneIndicatoreAsync(getRequest(fiscalCode, iseeType), asyncHandler));
    }

    private ConsultazioneIndicatoreRequestType getRequest(String fiscalCode, IseeTypologyEnum iseeType) {
        ConsultazioneIndicatoreRequestType consultazioneIndicatoreRequestType = new ConsultazioneIndicatoreRequestType();

        RicercaCFType ricercaCFType = new RicercaCFType();
        ricercaCFType.setCodiceFiscale(fiscalCode);
        ricercaCFType.setPrestazioneDaErogare(PrestazioneDaErogareType.fromValue(serviceToBeProvided));
        ricercaCFType.setProtocolloDomandaEnteErogatore(requestProtocolEnte);
        ricercaCFType.setStatodomandaPrestazione(StatoDomandaPrestazioneType.fromValue(requestStateEnte));

        consultazioneIndicatoreRequestType.setRicercaCF(ricercaCFType);
        consultazioneIndicatoreRequestType.setTipoIndicatore(transcodeIseeType(iseeType));

        return consultazioneIndicatoreRequestType;
    }

    private static TipoIndicatoreSinteticoEnum transcodeIseeType(IseeTypologyEnum iseeType) {
        return switch (iseeType) {
            case CORRENTE, ORDINARIO -> TipoIndicatoreSinteticoEnum.ORDINARIO; // TODO what's supposed to be CORRENTE?
            case MINORENNE -> TipoIndicatoreSinteticoEnum.MINORENNE;
            case UNIVERSITARIO -> TipoIndicatoreSinteticoEnum.UNIVERSITARIO;
            case SOCIOSANITARIO -> TipoIndicatoreSinteticoEnum.SOCIO_SANITARIO;
            case DOTTORATO -> TipoIndicatoreSinteticoEnum.DOTTORATO;
            case RESIDENZIALE -> TipoIndicatoreSinteticoEnum.RESIDENZIALE;
        };
    }

}
