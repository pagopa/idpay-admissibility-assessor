package it.gov.pagopa.admissibility.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;

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
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.function.Consumer;

@Slf4j
public final class Utils {
    private Utils(){}

    /** It will try to deserialize a message, eventually notifying the error  */
    public static <T> T deserializeMessage(Message<?> message, ObjectReader objectReader, Consumer<Throwable> onError) {
        try {
            String payload;
            if(message.getPayload() instanceof byte[] bytes){
                payload=new String(bytes);
            } else {
                payload=message.getPayload().toString();
            }
            return objectReader.readValue(payload);
        } catch (JsonProcessingException e) {
            onError.accept(e);
            return null;
        }
    }

    public static X509Certificate getCertificate(String cert) throws IOException, CertificateException {
        log.debug("start getCertificate");
        try(
                InputStream is = new ByteArrayInputStream(cert.getBytes(StandardCharsets.UTF_8))
        ) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(is);
        }
    }

    public static RSAPrivateKey getPrivateKey(String privateKey) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        log.debug("start getPrivateKey");
        String keyStringFormat =  pemToString(privateKey);
        try(
                InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(keyStringFormat))
        ) {
            PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(is.readAllBytes());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(encodedKeySpec);
        }
    }

    public static String pemToString(String target) {
        return target
                .replaceAll("^-----BEGIN[A-Z|\\s]+-----", "")
                .replaceAll("\\n+", "")
                .replaceAll("-----END[A-Z|\\s]+-----$", "");
    }
}
