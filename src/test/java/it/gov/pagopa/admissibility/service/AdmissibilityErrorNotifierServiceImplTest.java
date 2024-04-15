package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.config.KafkaConfiguration;
import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class AdmissibilityErrorNotifierServiceImplTest {
    private static final String BINDER_KAFKA_TYPE = "kafka";
    private static final String BINDER_BROKER = "broker";
    private static final String DUMMY_MESSAGE = "DUMMY MESSAGE";
    private static final Message<String> dummyMessage = MessageBuilder.withPayload(DUMMY_MESSAGE).build();
    private static Map<String, KafkaConfiguration.KafkaInfoDTO> bindingsMap;

    @Mock
    private ErrorNotifierService errorNotifierServiceMock;
    @Mock
    private KafkaConfiguration kafkaConfigurationMock;

    private AdmissibilityErrorNotifierServiceImpl admissibilityErrorNotifierService;

    @BeforeEach
    void setUp() {
        admissibilityErrorNotifierService = new AdmissibilityErrorNotifierServiceImpl(
                errorNotifierServiceMock,
                kafkaConfigurationMock,
                BINDER_BROKER
        );
        bindingsMap = Map.of(
                "beneficiaryRuleBuilderConsumer-in-0", KafkaConfiguration.KafkaInfoDTO.builder()
                        .type(BINDER_KAFKA_TYPE)
                        .brokers(BINDER_BROKER)
                        .destination( "beneficiary-rule-topic")
                        .group("beneficiary-rule-group")
                        .build(),
                "admissibilityProcessor-in-0", KafkaConfiguration.KafkaInfoDTO.builder()
                        .destination("admissibility-topic")
                        .group("admissibility-group")
                        .type(BINDER_KAFKA_TYPE)
                        .brokers("ServiceBus")
                        .binder("kafka-onboarding-request")
                        .build(),
                "admissibilityProcessorOut-out-0", KafkaConfiguration.KafkaInfoDTO.builder()
                        .type(BINDER_KAFKA_TYPE)
                        .brokers(BINDER_BROKER)
                        .destination( "admissibility-out-topic")
                        .build(),
                "rankingRequest-out-0",KafkaConfiguration.KafkaInfoDTO.builder()
                        .type(BINDER_KAFKA_TYPE)
                        .brokers(BINDER_BROKER)
                        .destination( "admissibility-ranking-request-topic")
                        .build(),
                "consumerCommands-in-0", KafkaConfiguration.KafkaInfoDTO.builder()
                        .type(BINDER_KAFKA_TYPE)
                        .brokers(BINDER_BROKER)
                        .destination( "commands-topic")
                        .group("commands-group")
                        .build()
        );

    }

    @Test
    void notifyBeneficiaryRuleBuilder() {
        Mockito.when(kafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(kafkaConfigurationMock.getStream().getBindings()).thenReturn(bindingsMap);

        errorNotifyMock(bindingsMap.get("beneficiaryRuleBuilderConsumer-in-0"),true,true);
        admissibilityErrorNotifierService.notifyBeneficiaryRuleBuilder(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock,kafkaConfigurationMock);
    }

    @Test
    void notifyAdmissibilityOutcome() {
        Mockito.when(kafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(kafkaConfigurationMock.getStream().getBindings()).thenReturn(bindingsMap);

        errorNotifyMock(bindingsMap.get("admissibilityProcessorOut-out-0"),true,false);
        admissibilityErrorNotifierService.notifyAdmissibilityOutcome(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock,kafkaConfigurationMock);
    }

    @Test
    void notifyRankingRequest() {
        Mockito.when(kafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(kafkaConfigurationMock.getStream().getBindings()).thenReturn(bindingsMap);

        errorNotifyMock(bindingsMap.get("rankingRequest-out-0"),true,false);
        admissibilityErrorNotifierService.notifyRankingRequest(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock,kafkaConfigurationMock);
    }

    @Test
    void notifyAdmissibilityCommands() {
        Mockito.when(kafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(kafkaConfigurationMock.getStream().getBindings()).thenReturn(bindingsMap);

        errorNotifyMock(bindingsMap.get("consumerCommands-in-0"),true,true);

        admissibilityErrorNotifierService.notifyAdmissibilityCommands(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock,kafkaConfigurationMock);
    }

    @Test
    void testNotify() {
        errorNotifyMock(bindingsMap.get("consumerCommands-in-0"),true,true);
        admissibilityErrorNotifierService.notify(bindingsMap.get("consumerCommands-in-0"),dummyMessage,DUMMY_MESSAGE,true,true,new Throwable(DUMMY_MESSAGE));
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock,kafkaConfigurationMock);
    }

    @Test
    void testAdmissibility() {
        KafkaConfiguration.BaseKafkaInfoDTO baseKafkaInfoDTO = new KafkaConfiguration.BaseKafkaInfoDTO(
                "admissibility-topic",
                "",
                BINDER_KAFKA_TYPE,
                "ServiceBus"
        );
        Mockito.when(errorNotifierServiceMock.notify(eq(baseKafkaInfoDTO), eq(dummyMessage), eq(DUMMY_MESSAGE), eq(true), eq(true), any()))
                .thenReturn(true);
        Mockito.when(kafkaConfigurationMock.getTopicForBindings("admissibilityProcessor-in-0")).thenReturn("admissibility-topic");
        Mockito.when(kafkaConfigurationMock.getTypeForBinder("kafka-onboarding-request")).thenReturn(BINDER_KAFKA_TYPE);

        admissibilityErrorNotifierService.notifyAdmissibility(dummyMessage,DUMMY_MESSAGE,true,new Throwable(DUMMY_MESSAGE));
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock,kafkaConfigurationMock);
    }

    private void errorNotifyMock(KafkaConfiguration.BaseKafkaInfoDTO basekafkaInfoDTO, boolean retryable, boolean resendApplication ) {
        Mockito.when(errorNotifierServiceMock.notify(eq(basekafkaInfoDTO), eq(dummyMessage), eq(DUMMY_MESSAGE), eq(retryable), eq(resendApplication), any()))
                .thenReturn(true);
    }
}