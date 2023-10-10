package it.gov.pagopa.common.soap.utils;

import com.sun.xml.ws.developer.JAXWSProperties;
import it.gov.pagopa.common.http.utils.JdkSslUtils;
import it.gov.pagopa.common.soap.service.SoapLoggingHandler;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.Handler;

import javax.net.ssl.SSLContext;
import java.util.List;

public class SoapUtils {
    private SoapUtils(){}

    public static void configureBaseUrl(BindingProvider bindingProvider, String baseUrl) {
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, baseUrl);
    }

    public static void configureTimeouts(BindingProvider bindingProvider, int connectTimeoutMs, int requestTimeoutMS) {
        if(connectTimeoutMs >0){
            bindingProvider.getRequestContext().put(JAXWSProperties.CONNECT_TIMEOUT, connectTimeoutMs);
        }

        if(requestTimeoutMS >0) {
            bindingProvider.getRequestContext().put(JAXWSProperties.REQUEST_TIMEOUT, requestTimeoutMS);
        }
    }

    public static void configureSSL(BindingProvider bindingProvider, String cert, String key, String trustCertCollectionString) {
        SSLContext sslContext = JdkSslUtils.buildSSLContext(cert, key, trustCertCollectionString);
        bindingProvider.getRequestContext().put(JAXWSProperties.SSL_SOCKET_FACTORY, sslContext.getSocketFactory());
    }

    public static void configureSoapLogging(BindingProvider bindingProvider, SoapLoggingHandler soapLoggingHandler) {
        @SuppressWarnings("rawtypes")
        List<Handler> handlerChain = bindingProvider.getBinding().getHandlerChain();
        handlerChain.add(soapLoggingHandler);
        bindingProvider.getBinding().setHandlerChain(handlerChain);
    }
}
