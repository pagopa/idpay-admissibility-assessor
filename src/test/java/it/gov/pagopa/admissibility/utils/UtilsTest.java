package it.gov.pagopa.admissibility.utils;

import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.admissibility.dto.ErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
class UtilsTest {

    private final ObjectReader errorDtoObjectReader = TestUtils.objectMapper.readerFor(ErrorDTO.class);

    @Test
    void testDeserializeStringMessageOnError(){
        testDeserializeMessageOnError("PROVA");
    }

    @Test
    void testDeserializeBytesMessageOnError(){
        testDeserializeMessageOnError("PROVA".getBytes(StandardCharsets.UTF_8));
    }

    private <T> void testDeserializeMessageOnError(T payload){
        // Given
        Message<T> stringMsg = MessageBuilder.createMessage(payload, new MessageHeaders(null));
        @SuppressWarnings("unchecked") Consumer<Throwable> onErrorMock = Mockito.mock(Consumer.class);

        // When
        ErrorDTO result = Utils.deserializeMessage(stringMsg, errorDtoObjectReader, onErrorMock);

        // Then
        Assertions.assertNull(result);
        Mockito.verify(onErrorMock).accept(Mockito.any());
    }

    @Test
    void testDeserializeStringMessage(){
        ErrorDTO expected = new ErrorDTO("CODE", "MESSAGE");
        testDeserializeMessage(TestUtils.jsonSerializer(expected), expected);
    }

    @Test
    void testDeserializeBytesMessage(){
        ErrorDTO expected = new ErrorDTO("CODE", "MESSAGE");
        testDeserializeMessage(TestUtils.jsonSerializer(expected).getBytes(StandardCharsets.UTF_8), expected);
    }

    private <T> void testDeserializeMessage(T payload, Object expectedDeserialized){
        // Given
        Message<T> stringMsg = MessageBuilder.createMessage(payload, new MessageHeaders(null));
        @SuppressWarnings("unchecked") Consumer<Throwable> onErrorMock = Mockito.mock(Consumer.class);

        // When
        ErrorDTO result = Utils.deserializeMessage(stringMsg, errorDtoObjectReader, onErrorMock);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedDeserialized, result);
        Mockito.verifyNoInteractions(onErrorMock);
    }

    @Test
    void testEuro2Cents(){
        Assertions.assertNull(Utils.euro2Cents(null));
        Assertions.assertEquals(100L, Utils.euro2Cents(BigDecimal.ONE));
        Assertions.assertEquals(325L, Utils.euro2Cents(BigDecimal.valueOf(3.25)));
    }

    @Test
    void testBirthDateFromFiscalCodeAndAge() {
        Map<String, LocalDate> mapFiscalCodesBirthDates = Map.of(
                "PTRGNL73S51X000Q", LocalDate.of(1973, 11, 11),
                "BLBGRC57A05X000D", LocalDate.of(1957, 1, 5),
                "DSUFLV00R19X000P", LocalDate.of(2000, 10, 19),
                "BCCVCR01E65X000T", LocalDate.of(2001, 5, 25)
        );

        for(String fc : mapFiscalCodesBirthDates.keySet()) {
            LocalDate result = Utils.calculateBirthDateFromFiscalCode(fc);

            Assertions.assertEquals(mapFiscalCodesBirthDates.get(fc), result);
        }

        int age = Utils.getAge(LocalDate.now().minusYears(20).plusDays(1));
        Assertions.assertEquals(19, age);
    }
}
