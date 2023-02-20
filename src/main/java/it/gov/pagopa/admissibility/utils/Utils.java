package it.gov.pagopa.admissibility.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
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
            String payload = readMessagePayload(message);
            return objectReader.readValue(payload);
        } catch (JsonProcessingException e) {
            onError.accept(e);
            return null;
        }
    }

    public static String readMessagePayload(Message<?> message) {
        String payload;
        if(message.getPayload() instanceof byte[] bytes){
            payload=new String(bytes);
        } else {
            payload= message.getPayload().toString();
        }
        return payload;
    }

    /** To read Message header value */
    @SuppressWarnings("unchecked")
    public static <T> T getHeaderValue(Message<?> message, String headerName) {
        return  (T)message.getHeaders().get(headerName);
    }

    public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    public static Long euro2Cents(BigDecimal euro){
        return euro == null? null : euro.multiply(ONE_HUNDRED).longValue();
    }

    public static <T> String convertToJson(T object, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error converting request in JSON",e);
        }
    }

    public static String createSHA256Digest(String request) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(request.getBytes(StandardCharsets.UTF_8));
            return "SHA-256="+ Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Something went wrong creating SHA256 digest", e);
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
