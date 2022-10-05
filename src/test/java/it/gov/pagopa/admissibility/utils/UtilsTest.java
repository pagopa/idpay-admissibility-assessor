package it.gov.pagopa.admissibility.utils;

import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.admissibility.dto.ErrorDTO;
import it.gov.pagopa.admissibility.exception.Severity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

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
        ErrorDTO expected = new ErrorDTO(Severity.ERROR, "TITLE", "MESSAGE");
        testDeserializeMessage(TestUtils.jsonSerializer(expected), expected);
    }

    @Test
    void testDeserializeBytesMessage(){
        ErrorDTO expected = new ErrorDTO(Severity.ERROR, "TITLE", "MESSAGE");
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
}
