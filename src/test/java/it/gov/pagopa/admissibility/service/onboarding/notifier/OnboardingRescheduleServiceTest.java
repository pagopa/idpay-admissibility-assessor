package it.gov.pagopa.admissibility.service.onboarding.notifier;

import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.service.AdmissibilityErrorNotifierService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.OffsetDateTime;

@ExtendWith(MockitoExtension.class)
class OnboardingRescheduleServiceTest {

    @Mock private StreamBridge streamBridgeMock;
    @Mock private AdmissibilityErrorNotifierService admissibilityErrorNotifierServiceMock;

    private OnboardingRescheduleService service;

    @BeforeEach
    void init(){
        service = new OnboardingRescheduleServiceImpl(streamBridgeMock, admissibilityErrorNotifierServiceMock);
    }

    @AfterEach
    void verifyNotMoreMockInvocations(){
        Mockito.verifyNoMoreInteractions(streamBridgeMock, admissibilityErrorNotifierServiceMock);
    }

    private final OnboardingDTO request = new OnboardingDTO();
    private final OffsetDateTime rescheduleDateTime = OffsetDateTime.now().plusMinutes(5);
    private final Message<String> requestMessage = MessageBuilder.withPayload("").setHeader("HEADER1", "VALUE1").build();

    @Test
    void test(){
        //Given
        Mockito.when(streamBridgeMock.send(Mockito.any(), Mockito.any())).thenReturn(true);

        // When
        callService();
    }

    @Test
    void testWhenError(){
        //Given
        Mockito.when(streamBridgeMock.send(Mockito.any(), Mockito.any())).thenReturn(false);

        // When
        callService();

        // Then
        Mockito.verify(admissibilityErrorNotifierServiceMock).notifyAdmissibility(Mockito.argThat(m -> {assertMessage(m); return true;}), Mockito.any(), Mockito.eq(true), Mockito.isNull());
    }

    private void callService() {
        // When
        service.reschedule(request, rescheduleDateTime, "CAUSE", requestMessage);

        // Then
        Mockito.verify(streamBridgeMock)
                .send(Mockito.eq("admissibilityDelayProducer-out-0"),
                        Mockito.argThat(i -> {
                            assertMessage(i);
                            return true;
                        }));
    }

    private void assertMessage(Object i) {
        if(i instanceof Message<?> message){
            Assertions.assertSame(request, message.getPayload());
            Assertions.assertSame(rescheduleDateTime, message.getHeaders().get(ServiceBusMessageHeaders.SCHEDULED_ENQUEUE_TIME));
            Assertions.assertEquals("VALUE1", message.getHeaders().get("HEADER1"));
        } else {
            Assertions.fail("Unexpected message sent type: " + i.getClass());
        }
    }
}
