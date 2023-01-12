package it.gov.pagopa.admissibility.soap.inps;

import com.sun.xml.ws.developer.WSBindingProvider;
import it.gov.pagopa.admissibility.generated.soap.ws.client.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.gov.pagopa.admissibility.soap.inps.ReactorAsyncHandler.into;

@Service
public class IseeConsultationSoapClientImpl implements IseeConsultationSoapClient{
    private final ISvcConsultazione portSvcConsultazione;
    private final String certInps;


    public IseeConsultationSoapClientImpl(@Value("${app.inps.header.officeCode}")String codiceUfficio,
                                          @Value("${app.inps.header.userId}")String userId,
                                          @Value("${app.inps.iseeConsultation.base-url}")String baseUrlINPS,
                                          @Value("${app.inps.secure.cert}") String certInps) {
        this.certInps = certInps;
        this.portSvcConsultazione = new SvcConsultazione().getPort(ISvcConsultazione.class);

        //Set header with info caller TODO TVB
        Identity identity = new Identity();
        identity.setCodiceUfficio(codiceUfficio);
        identity.setUserId(userId);

        WSBindingProvider bp = (WSBindingProvider) portSvcConsultazione;
        bp.setOutboundHeaders(identity);

        //Set baseUrl
        bp.setAddress(baseUrlINPS);
    }

    @Override
    public Mono<ConsultazioneIndicatoreResponse> callService(String fiscalCode){
        //set sslContext
        return Mono.create(
                sink -> portSvcConsultazione.consultazioneIndicatoreAsync(getRequest(fiscalCode), into(sink))); //TODO confirm operation to call
    }

//    private void authenticateMessage() throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {
////        SSLContext sslContext = SSLContext.getInstance("SSL");
////        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
////        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
////        keyStore.load(getCertInputStream(certInps),null); //file
////        keyManagerFactory.init(keyStore,null);
////        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
////
////
////        WSBindingProvider bp = (WSBindingProvider) portSvcConsultazione;
////        bp.getRequestContext().put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", sslContext.getSocketFactory());
//    }

    private ConsultazioneIndicatoreRequestType getRequest(String fiscalCode)
    {
        ConsultazioneIndicatoreRequestType consultazioneIndicatoreRequestType = new ConsultazioneIndicatoreRequestType();

        RicercaCFType ricercaCFType = new RicercaCFType();
        ricercaCFType.setCodiceFiscale(fiscalCode);
        consultazioneIndicatoreRequestType.setRicercaCF(ricercaCFType);

        consultazioneIndicatoreRequestType.setTipoIndicatore(TipoIndicatoreSinteticoEnum.ORDINARIO); //TODO TBV default value

        return consultazioneIndicatoreRequestType;
    }
}
