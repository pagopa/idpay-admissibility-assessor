package it.gov.pagopa.common.http.utils;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import it.gov.pagopa.common.crypto.utils.CertUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;

public class JdkSslUtils {
    private JdkSslUtils() {}

    public static final String TRUST_ALL = "TRUST_ALL";

    public static SSLContext buildSSLContext(String certString, String privateKeyString, String trustCertCollectionString) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");

            sslContext.init(
                    getKeyManagers(certString, privateKeyString),
                    getTrustManagers(trustCertCollectionString), null);

            return sslContext;
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | IOException | InvalidKeySpecException | KeyStoreException | KeyManagementException e) {
            throw new IllegalStateException("Something went wrong creating JDK ssl context",e);
        }
    }

    public static KeyStore buildEmptyKeyStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null);
        return keyStore;
    }

    private static KeyManager[] getKeyManagers(String certString, String privateKeyString) throws IOException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        KeyStore keyStore = buildEmptyKeyStore();

        X509Certificate cert = CertUtils.pemCert2Certificate(certString);
        RSAPrivateKey pKey = CertUtils.pemKey2PrivateKey(privateKeyString);

        keyStore.setCertificateEntry("cert-alias", cert);
        keyStore.setKeyEntry("key-alias", pKey, "".toCharArray(), new Certificate[]{cert});

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "".toCharArray());
        return keyManagerFactory.getKeyManagers();
    }

    private static TrustManager[] getTrustManagers(String trustCertCollectionString) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        if(TRUST_ALL.equals(trustCertCollectionString)){
            return InsecureTrustManagerFactory.INSTANCE.getTrustManagers();
        } else {
            KeyStore keyStore = buildEmptyKeyStore();
            int i=0;
            for(String certString : extractCertificates(trustCertCollectionString)){
                X509Certificate cert = CertUtils.pemCert2Certificate(certString);

                keyStore.setCertificateEntry("certificate-"+ i++, cert);
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            return trustManagerFactory.getTrustManagers();
        }
    }

    private static String[] extractCertificates(String trustCertCollectionString) {
        return trustCertCollectionString.replaceAll("(-+END\\s*CERTIFICATE\\s*-+\\s*)", "$1###").split("###");
    }
}
