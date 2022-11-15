package it.gov.pagopa.admissibility.soap.inps;

import it.gov.pagopa.admissibility.generated.soap.ws.client.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.xml.namespace.QName;

import static it.gov.pagopa.admissibility.soap.inps.ReactorAsyncHandler.into;

@Service
public class IseeConsultationSoapClientImpl implements IseeConsultationSoapClient{
    private final ISvcConsultazione portSvcConsultazione;

    public IseeConsultationSoapClientImpl() {
        this.portSvcConsultazione = new SvcConsultazione().getPort(ISvcConsultazione.class);
    }

    @Override
    public Mono<ConsultazioneIndicatoreResponse> callService(String fiscalCode){
        return Mono.create(
                sink -> portSvcConsultazione.consultazioneIndicatoreAsync(getRequest(fiscalCode), into(sink))); //TODO confirm operation to call
    }

    private ConsultazioneIndicatoreRequestType getRequest(String fiscalCode)
    {
        ConsultazioneIndicatoreRequestType consultazioneIndicatoreRequestType = new ConsultazioneIndicatoreRequestType();

        RicercaCFType ricercaCFType = new RicercaCFType();
        ricercaCFType.setCodiceFiscale(fiscalCode);
        consultazioneIndicatoreRequestType.setRicercaCF(ricercaCFType);

        consultazioneIndicatoreRequestType.setTipoIndicatore(TipoIndicatoreSinteticoEnum.ORDINARIO); //TODO TBV default value

        return consultazioneIndicatoreRequestType;
    }

    //TODO create a custom Header to send in request for userId and officeCode
//    private void setHttpHeaders(String fiscalCode){
//        Map<String, List<String>> requestHeaders = new HashMap<>();
//
//        BindingProvider bindingProvider = (BindingProvider) portSvcConsultazione;
//        bindingProvider.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS,requestHeaders);
//    }

    //TODO set base-url from application.yml
//    private ISvcConsultazione getClient(String inpsBaseUrl){
//        QName qName= new QName(inpsBaseUrl,"SvcConsultazione");
//        SvcConsultazione svcConsultazione = new SvcConsultazione(null,qName);
//        return svcConsultazione.getPort(ISvcConsultazione.class);
//    }
}
