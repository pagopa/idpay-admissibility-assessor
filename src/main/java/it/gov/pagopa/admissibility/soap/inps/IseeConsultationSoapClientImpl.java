package it.gov.pagopa.admissibility.soap.inps;

import com.sun.xml.ws.developer.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import it.gov.pagopa.admissibility.generated.soap.ws.client.Identity;
import it.gov.pagopa.admissibility.generated.soap.ws.client.*;
import it.gov.pagopa.admissibility.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.xml.ws.BindingProvider;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;

import static it.gov.pagopa.admissibility.soap.inps.ReactorAsyncHandler.into;

@Service
@Slf4j
public class IseeConsultationSoapClientImpl implements IseeConsultationSoapClient {
    private final ISvcConsultazione portSvcConsultazione;
    private final String certInps;
    private final String keyInps;


    public IseeConsultationSoapClientImpl(@Value("${app.inps.header.officeCode}") String codiceUfficio,
                                          @Value("${app.inps.header.userId}") String userId,
                                          @Value("${app.inps.iseeConsultation.base-url}") String baseUrlINPS,
                                          @Value("${app.inps.secure.cert}") String certInps,
                                          @Value("${app.inps.secure.key}") String keyInps,
                                          @Value("${app.inps.iseeConsultation.config.connection-timeout}") int connectTimeoutMs,
                                          @Value("${app.inps.iseeConsultation.config.request-timeout}") int requestTimeoutMS) throws NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, KeyManagementException, InvalidKeySpecException {
        this.certInps = certInps;
        this.keyInps = keyInps;
        this.portSvcConsultazione = new SvcConsultazione().getPort(ISvcConsultazione.class);

        Identity identity = new Identity();
        identity.setCodiceUfficio(codiceUfficio);
        identity.setUserId(userId);
        ((WSBindingProvider) portSvcConsultazione).setOutboundHeaders(identity);
        ((WSBindingProvider) portSvcConsultazione).setAddress(baseUrlINPS);

        ((BindingProvider) portSvcConsultazione).getRequestContext().put(JAXWSProperties.CONNECT_TIMEOUT, connectTimeoutMs);
        ((BindingProvider) portSvcConsultazione).getRequestContext().put(JAXWSProperties.REQUEST_TIMEOUT, requestTimeoutMS);
        settingSSL();
    }

    @Override
    public Mono<ConsultazioneIndicatoreResponse> callService(String fiscalCode) {
        //set sslContext
        return Mono.create(
                sink -> portSvcConsultazione.consultazioneIndicatoreAsync(getRequest(fiscalCode), into(sink))); //TODO confirm operation to call
    }

    private ConsultazioneIndicatoreRequestType getRequest(String fiscalCode) {
        ConsultazioneIndicatoreRequestType consultazioneIndicatoreRequestType = new ConsultazioneIndicatoreRequestType();

        RicercaCFType ricercaCFType = new RicercaCFType();
        ricercaCFType.setCodiceFiscale(fiscalCode);
        consultazioneIndicatoreRequestType.setRicercaCF(ricercaCFType);

        consultazioneIndicatoreRequestType.setTipoIndicatore(TipoIndicatoreSinteticoEnum.ORDINARIO); //TODO TBV value

        return consultazioneIndicatoreRequestType;
    }

    private void settingSSL() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, UnrecoverableKeyException, KeyManagementException, InvalidKeySpecException {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        X509Certificate cert = Utils.getCertificate(certInps);
        RSAPrivateKey pKey = Utils.getPrivateKey(keyInps);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null);
        keyStore.setCertificateEntry("cert-alias", cert);
        keyStore.setKeyEntry("key-alias", pKey, "".toCharArray(), new Certificate[]{cert});

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "".toCharArray());

        sslContext.init(keyManagerFactory.getKeyManagers(), InsecureTrustManagerFactory.INSTANCE.getTrustManagers(), null);

        ((BindingProvider) portSvcConsultazione).getRequestContext().put(JAXWSProperties.SSL_SOCKET_FACTORY, sslContext.getSocketFactory());
    }
}
