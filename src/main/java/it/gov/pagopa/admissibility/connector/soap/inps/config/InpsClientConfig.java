package it.gov.pagopa.admissibility.connector.soap.inps.config;

import com.sun.xml.ws.developer.WSBindingProvider;
import it.gov.pagopa.admissibility.config.InpsConfiguration;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ISvcConsultazione;
import it.gov.pagopa.admissibility.generated.soap.ws.client.Identity;
import it.gov.pagopa.admissibility.generated.soap.ws.client.SvcConsultazione;
import it.gov.pagopa.common.http.utils.JdkSslUtils;
import it.gov.pagopa.common.soap.service.SoapLoggingHandler;
import it.gov.pagopa.common.soap.utils.SoapUtils;
import jakarta.xml.ws.BindingProvider;
import lombok.Data;
import org.springframework.stereotype.Service;

@Service
@Data
public class InpsClientConfig {
    private final ISvcConsultazione portSvcConsultazione;
    private final InpsConfiguration inpsConfiguration;

    public InpsClientConfig(SoapLoggingHandler soapLoggingHandler,
                            InpsConfiguration inpsConfiguration)  {
        this.inpsConfiguration = inpsConfiguration;
        this.portSvcConsultazione = new SvcConsultazione().getPort(ISvcConsultazione.class);

        it.gov.pagopa.admissibility.generated.soap.ws.client.Identity identity = new Identity();
        identity.setCodiceUfficio(inpsConfiguration.getOfficeCodeForInps());
        identity.setUserId(inpsConfiguration.getUserIdForInps());
        ((WSBindingProvider) portSvcConsultazione).setOutboundHeaders(identity);

        SoapUtils.configureBaseUrl(((BindingProvider) portSvcConsultazione), inpsConfiguration.getBaseUrlForInps());
        SoapUtils.configureTimeouts(((BindingProvider) portSvcConsultazione), inpsConfiguration.getConnectionTimeoutForInps(), inpsConfiguration.getRequestTimeoutForInps());
        SoapUtils.configureSSL(((BindingProvider) portSvcConsultazione), inpsConfiguration.getCertForInps(), inpsConfiguration.getKeyForInps(), JdkSslUtils.TRUST_ALL);
        SoapUtils.configureSoapLogging(((BindingProvider) portSvcConsultazione), soapLoggingHandler);
    }


}
