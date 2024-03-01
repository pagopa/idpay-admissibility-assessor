package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdmissibilityErrorNotifierServiceImplTest {
    private static final String BINDER_KAFKA_TYPE="kafka";
    private static final String BINDER_BROKER="broker";
    private static final String DUMMY_MESSAGE="DUMMY MESSAGE";
    private static final Message<String> dummyMessage = MessageBuilder.withPayload(DUMMY_MESSAGE).build();
    @Mock
    private ErrorNotifierService errorNotifierServiceMock;

    private AdmissibilityErrorNotifierServiceImpl admissibilityErrorNotifierService;

    @BeforeEach
    void setUp() {
        admissibilityErrorNotifierService = new AdmissibilityErrorNotifierServiceImpl(
                errorNotifierServiceMock,
                BINDER_KAFKA_TYPE,
                BINDER_BROKER,

                "beneficiary-rule-topic",
                "beneficiary-rule-group",
                BINDER_KAFKA_TYPE,
                BINDER_BROKER,

                "admissibility-topic",
                "admissibility-group",

                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                "admissibility-out-topic",

                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                "admissibility-ranking-request-topic",

                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                "commands-topic",
                "commands-group"
        );
    }

    @Test
    void notifyBeneficiaryRuleBuilder() {
        errorNotifyMock("beneficiary-rule-topic","beneficiary-rule-group",true,true);
        admissibilityErrorNotifierService.notifyBeneficiaryRuleBuilder(dummyMessage,DUMMY_MESSAGE,true,new Throwable(DUMMY_MESSAGE));

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyAdmissibilityOutcome() {
        errorNotifyMock("admissibility-out-topic",null,true,false);
        admissibilityErrorNotifierService.notifyAdmissibilityOutcome(dummyMessage,DUMMY_MESSAGE,true,new Throwable(DUMMY_MESSAGE));
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }

    @Test
    void notifyRankingRequest() {
        errorNotifyMock("admissibility-ranking-request-topic",null,true,false);
        admissibilityErrorNotifierService.notifyRankingRequest(dummyMessage,DUMMY_MESSAGE,true,new Throwable(DUMMY_MESSAGE));
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }

    @Test
    void notifyAdmissibilityCommands() {
        errorNotifyMock("commands-topic","commands-group",true,true);
        admissibilityErrorNotifierService.notifyAdmissibilityCommands(dummyMessage,DUMMY_MESSAGE,true,new Throwable(DUMMY_MESSAGE));
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }

    @Test
    void testNotify() {
        errorNotifyMock("commands-topic","commands-group",true,true);
        admissibilityErrorNotifierService.notify(BINDER_KAFKA_TYPE,BINDER_BROKER,"commands-topic","commands-group",dummyMessage,DUMMY_MESSAGE,true,true,new Throwable(DUMMY_MESSAGE));
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }

    @Test
    void testAdmissibility() {
        Mockito.when(errorNotifierServiceMock.notify(eq(BINDER_KAFKA_TYPE), eq("ServiceBus"),
                        eq("admissibility-topic"), eq("admissibility-group"), eq(dummyMessage), eq(DUMMY_MESSAGE), eq(true), eq(true), any()))
                .thenReturn(true);
        admissibilityErrorNotifierService.notifyAdmissibility(dummyMessage,DUMMY_MESSAGE,true,new Throwable(DUMMY_MESSAGE));
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }

    private void errorNotifyMock(String topic, String group, boolean retryable, boolean resendApplication ) {
        Mockito.when(errorNotifierServiceMock.notify(eq(BINDER_KAFKA_TYPE), eq(BINDER_BROKER),
                        eq(topic), eq(group), eq(dummyMessage), eq(DUMMY_MESSAGE), eq(retryable), eq(resendApplication), any()))
                .thenReturn(true);
    }
}