package it.gov.pagopa.common.http.utils;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class NettySslUtils {
    private NettySslUtils(){}

    public static final String TRUST_ALL = "TRUST_ALL";

    public static SslContext buildSSLContext(String cert, String privateKey, String trustCertCollectionString) {
        try(
                InputStream certInputStream = string2InputStream(cert);
                InputStream keyInputStream = string2InputStream(privateKey)
        ) {
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                    .keyManager(certInputStream, keyInputStream);
            return getSslContext(sslContextBuilder, trustCertCollectionString);
        } catch (IOException e) {
            throw new IllegalStateException("Something went wrong creating Netty ssl context",e);
        }
    }

    public static SslContext buildSSLContext(String trustCertCollectionString) {
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(JdkSslUtils.buildEmptyKeyStore(), "".toCharArray());
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                    .keyManager(keyManagerFactory);
            return getSslContext(sslContextBuilder, trustCertCollectionString);
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException |
                 UnrecoverableKeyException e) {
            throw new IllegalStateException("Something went wrong creating Netty ssl context",e);
        }
    }

    private static SslContext getSslContext(SslContextBuilder sslContextBuilder, String trustCertCollectionString) throws SSLException {
        if(!TRUST_ALL.equals(trustCertCollectionString)){
            try(InputStream trustCertCollectionInputStream = string2InputStream(trustCertCollectionString)){
                return sslContextBuilder.trustManager(trustCertCollectionInputStream).build();
            } catch (IOException e) {
                throw new IllegalStateException("Something went wrong reading trust certificates",e);
            }
        } else {
            return sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        }
    }

    private static InputStream string2InputStream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
