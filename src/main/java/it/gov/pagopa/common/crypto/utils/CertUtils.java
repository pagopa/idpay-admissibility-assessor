package it.gov.pagopa.common.crypto.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CertUtils {
    private CertUtils(){}

    public static X509Certificate pemCert2Certificate(String cert) throws IOException, CertificateException {
        try(
                InputStream is = new ByteArrayInputStream(cert.getBytes(StandardCharsets.UTF_8))
        ) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(is);
        }
    }

    public static RSAPrivateKey pemKey2PrivateKey(String privateKey) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        String keyStringFormat =  extractInlinePemBody(privateKey);
        try(
                InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(keyStringFormat))
        ) {
            PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(is.readAllBytes());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(encodedKeySpec);
        }
    }

    public static RSAPublicKey pemPub2PublicKey(String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        String pubStringFormat = extractInlinePemBody(publicKey);
        try(
                InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(pubStringFormat))
        ) {
            X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(is.readAllBytes());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(encodedKeySpec);
        }
    }

    public static String extractInlinePemBody(String target) {
        return target
                .replaceAll("^-----BEGIN[A-Z|\\s]+-----", "")
                .replaceAll("\\n+", "")
                .replaceAll("-----END[A-Z|\\s]+-----$", "");
    }
}
