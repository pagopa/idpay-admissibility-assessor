package it.gov.pagopa.admissibility.soap.inps;

import it.gov.pagopa.admissibility.generated.soap.ws.client.*;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.gov.pagopa.admissibility.soap.inps.ReactorAsyncHandler.into;

@Service
@Slf4j
public class IseeConsultationSoapClientImpl implements IseeConsultationSoapClient {
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
        return callService(fiscalCode, iseeType)
                .flatMap(response -> {
                    ConsultazioneIndicatoreResponseType result = response.getConsultazioneIndicatoreResult();
                    if (result.getEsito() != EsitoEnum.OK) {
                        //TODO Define what to do when result is different from ok
                        return Mono.empty();
                    } else {
                        return Mono.just(result);
                    }
                });
    }

    public Mono<ConsultazioneIndicatoreResponse> callService(String fiscalCode, IseeTypologyEnum iseeType) {
        return Mono.create(
                sink -> portSvcConsultazione.consultazioneIndicatoreAsync(getRequest(fiscalCode, iseeType), into(sink))); //TODO confirm operation to call
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
            case ORDINARIO -> TipoIndicatoreSinteticoEnum.ORDINARIO;
            case MINORENNE -> TipoIndicatoreSinteticoEnum.MINORENNE;
            case UNIVERSITARIO -> TipoIndicatoreSinteticoEnum.UNIVERSITARIO;
            case SOCIOSANITARIO -> TipoIndicatoreSinteticoEnum.SOCIO_SANITARIO;
            case DOTTORATO -> TipoIndicatoreSinteticoEnum.DOTTORATO;
            case RESIDENZIALE -> TipoIndicatoreSinteticoEnum.RESIDENZIALE;
            case CORRENTE -> null; // TODO what's supposed to be CORRENTE?
        };
    }

}
