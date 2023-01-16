package it.gov.pagopa.admissibility.soap.inps;

import it.gov.pagopa.admissibility.generated.soap.ws.client.*;
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
    private final String requestIndicatorType;

    public IseeConsultationSoapClientImpl(InpsClientConfig inpsClientConfig,
                                          @Value("${app.inps.request.request-protocol-ente}") String requestProtocolEnte,
                                          @Value("${app.inps.request.service-to-be-provided}")String serviceToBeProvided,
                                          @Value("${app.inps.request.request-state}")String requestStateEnte,
                                          @Value("${app.inps.request.request-indicator-type}")String requestIndicatorType){

        this.requestProtocolEnte = requestProtocolEnte;
        this.serviceToBeProvided = serviceToBeProvided;
        this.requestStateEnte = requestStateEnte;
        this.portSvcConsultazione = inpsClientConfig.getPortSvcConsultazione();
        this.requestIndicatorType = requestIndicatorType;
    }

    @Override
    public Mono<ConsultazioneIndicatoreResponse> callService(String fiscalCode) {
        return Mono.create(
                sink -> portSvcConsultazione.consultazioneIndicatoreAsync(getRequest(fiscalCode), into(sink))); //TODO confirm operation to call
    }

    private ConsultazioneIndicatoreRequestType getRequest(String fiscalCode) {
        ConsultazioneIndicatoreRequestType consultazioneIndicatoreRequestType = new ConsultazioneIndicatoreRequestType();

        RicercaCFType ricercaCFType = new RicercaCFType();
        ricercaCFType.setCodiceFiscale(fiscalCode);
        ricercaCFType.setPrestazioneDaErogare(PrestazioneDaErogareType.fromValue(serviceToBeProvided));
        ricercaCFType.setProtocolloDomandaEnteErogatore(requestProtocolEnte);
        ricercaCFType.setStatodomandaPrestazione(StatoDomandaPrestazioneType.fromValue(requestStateEnte));

        consultazioneIndicatoreRequestType.setRicercaCF(ricercaCFType);
        consultazioneIndicatoreRequestType.setTipoIndicatore(TipoIndicatoreSinteticoEnum.fromValue(requestIndicatorType));

        return consultazioneIndicatoreRequestType;
    }
}
