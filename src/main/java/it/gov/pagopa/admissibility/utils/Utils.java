package it.gov.pagopa.admissibility.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import org.springframework.messaging.Message;

import java.math.BigDecimal;
import java.util.function.Consumer;

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

    public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    public static Long euro2Cents(BigDecimal euro){
        return euro == null? null : euro.multiply(ONE_HUNDRED).longValue();
    }
}
