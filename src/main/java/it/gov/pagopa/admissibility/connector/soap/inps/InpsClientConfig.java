package it.gov.pagopa.admissibility.connector.soap.inps;

import com.sun.xml.ws.developer.JAXWSProperties;
import com.sun.xml.ws.developer.WSBindingProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import it.gov.pagopa.admissibility.connector.soap.inps.utils.SoapLoggingHandler;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ISvcConsultazione;
import it.gov.pagopa.admissibility.generated.soap.ws.client.Identity;
import it.gov.pagopa.admissibility.generated.soap.ws.client.SvcConsultazione;
import it.gov.pagopa.admissibility.utils.Utils;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.Handler;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

@Service
@Data
public class InpsClientConfig {
    private final ISvcConsultazione portSvcConsultazione;
    private final String certInps;
    private final String keyInps;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    public InpsClientConfig(@Value("${app.inps.header.officeCode}") String codiceUfficio,
                            @Value("${app.inps.header.userId}") String userId,
                            @Value("${app.inps.iseeConsultation.base-url}") String baseUrlINPS,
                            @Value("${app.inps.secure.cert}") String certInps,
                            @Value("${app.inps.secure.key}") String keyInps,
                            @Value("${app.inps.iseeConsultation.config.connection-timeout}") int connectTimeoutMs,
                            @Value("${app.inps.iseeConsultation.config.request-timeout}") int requestTimeoutMS,

                            SoapLoggingHandler soapLoggingHandler) throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, InvalidKeySpecException, KeyManagementException {
        this.certInps = certInps;
        this.keyInps = keyInps;
        this.portSvcConsultazione = new SvcConsultazione().getPort(ISvcConsultazione.class);

        it.gov.pagopa.admissibility.generated.soap.ws.client.Identity identity = new Identity();
        identity.setCodiceUfficio(codiceUfficio);
        identity.setUserId(userId);
        ((WSBindingProvider) portSvcConsultazione).setOutboundHeaders(identity);

        ((BindingProvider) portSvcConsultazione).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, baseUrlINPS);

        if(connectTimeoutMs>0){
            ((BindingProvider) portSvcConsultazione).getRequestContext().put(JAXWSProperties.CONNECT_TIMEOUT, connectTimeoutMs);
        }

        if(requestTimeoutMS>0) {
            ((BindingProvider) portSvcConsultazione).getRequestContext().put(JAXWSProperties.REQUEST_TIMEOUT, requestTimeoutMS);
        }

        settingSSL();

        @SuppressWarnings("rawtypes")
        List<Handler> handlerChain = ((BindingProvider) portSvcConsultazione).getBinding().getHandlerChain();
        handlerChain.add(soapLoggingHandler);
        ((BindingProvider) portSvcConsultazione).getBinding().setHandlerChain(handlerChain);
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