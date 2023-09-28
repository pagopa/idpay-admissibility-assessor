package it.gov.pagopa.admissibility.connector.soap.inps.config;

import com.sun.xml.ws.developer.WSBindingProvider;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ISvcConsultazione;
import it.gov.pagopa.admissibility.generated.soap.ws.client.Identity;
import it.gov.pagopa.admissibility.generated.soap.ws.client.SvcConsultazione;
import it.gov.pagopa.common.http.utils.JdkSslUtils;
import it.gov.pagopa.common.soap.service.SoapLoggingHandler;
import it.gov.pagopa.common.soap.utils.SoapUtils;
import jakarta.xml.ws.BindingProvider;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Data
public class InpsClientConfig {
    private final ISvcConsultazione portSvcConsultazione;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    public InpsClientConfig(@Value("${app.inps.header.officeCode}") String codiceUfficio,
                            @Value("${app.inps.header.userId}") String userId,
                            @Value("${app.inps.iseeConsultation.base-url}") String baseUrlINPS,
                            @Value("${app.inps.secure.cert}") String certInps,
                            @Value("${app.inps.secure.key}") String keyInps,
                            @Value("${app.inps.iseeConsultation.config.connection-timeout}") int connectTimeoutMs,
                            @Value("${app.inps.iseeConsultation.config.request-timeout}") int requestTimeoutMS,

                            SoapLoggingHandler soapLoggingHandler)  {
        this.portSvcConsultazione = new SvcConsultazione().getPort(ISvcConsultazione.class);

        it.gov.pagopa.admissibility.generated.soap.ws.client.Identity identity = new Identity();
        identity.setCodiceUfficio(codiceUfficio);
        identity.setUserId(userId);
        ((WSBindingProvider) portSvcConsultazione).setOutboundHeaders(identity);

        SoapUtils.configureBaseUrl(((BindingProvider) portSvcConsultazione), baseUrlINPS);
        SoapUtils.configureTimeouts(((BindingProvider) portSvcConsultazione), connectTimeoutMs, requestTimeoutMS);
        SoapUtils.configureSSL(((BindingProvider) portSvcConsultazione), certInps, keyInps, JdkSslUtils.TRUST_ALL);
        SoapUtils.configureSoapLogging(((BindingProvider) portSvcConsultazione), soapLoggingHandler);
    }


}
