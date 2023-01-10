package it.gov.pagopa.admissibility.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.springframework.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.math.BigDecimal;
import java.util.function.Consumer;

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
}
