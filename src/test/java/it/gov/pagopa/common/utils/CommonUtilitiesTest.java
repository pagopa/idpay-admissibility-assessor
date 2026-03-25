package it.gov.pagopa.common.utils;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class CommonUtilitiesTest {

    private final ObjectReader errorDtoObjectReader = TestUtils.objectMapper.readerFor(ErrorDTO.class);

    @Test
    void testConvertOk() {
        byte[] payloadBytes = "valid-json".getBytes();
        Message<byte[]> message = MessageBuilder.withPayload(payloadBytes).build();
        ObjectMapper objectMapper = new ObjectMapper();
        Object result = CommonUtilities.convertToJson(message, objectMapper);
        assertNotNull(result);
    }



    @Test
    void convertToJson_shouldThrowException() {
        ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);

        Object input = new Object();

        Mockito.when(objectMapper.writeValueAsString(input))
                .thenThrow(new JacksonException("error") {});

        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> CommonUtilities.convertToJson(input, objectMapper)
        );

        Assertions.assertEquals("Error converting request in JSON", ex.getMessage());
        Assertions.assertNotNull(ex.getCause());
    }

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
        ErrorDTO result = CommonUtilities.deserializeMessage(stringMsg, errorDtoObjectReader, onErrorMock);

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
        ErrorDTO result = CommonUtilities.deserializeMessage(stringMsg, errorDtoObjectReader, onErrorMock);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedDeserialized, result);
        Mockito.verifyNoInteractions(onErrorMock);
    }

    @Test
    void testCentsToEuro(){
        Assertions.assertEquals(
                BigDecimal.valueOf(5).setScale(2, RoundingMode.UNNECESSARY),
                CommonUtilities.centsToEuro(5_00L)
        );
    }

    @Test
    void testEuroToCents(){
        Assertions.assertNull(CommonUtilities.euroToCents(null));
        Assertions.assertEquals(100L, CommonUtilities.euroToCents(BigDecimal.ONE));
        Assertions.assertEquals(325L, CommonUtilities.euroToCents(BigDecimal.valueOf(3.25)));

        Assertions.assertEquals(
                5_00L,
                CommonUtilities.euroToCents(TestUtils.bigDecimalValue(5))
        );
    }

}
