package it.gov.pagopa.admissibility.service.commands;

import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.admissibility.dto.commands.QueueCommandOperationDTO;
import it.gov.pagopa.admissibility.service.AdmissibilityErrorNotifierService;
import it.gov.pagopa.admissibility.service.commands.operations.DeleteInitiativeService;
import it.gov.pagopa.admissibility.service.onboarding.OnboardingContextHolderService;
import it.gov.pagopa.admissibility.utils.CommandConstants;
import it.gov.pagopa.common.utils.MemoryAppender;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.KieBase;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class CommandMediatorServiceImplTest {

    @Mock
    private AdmissibilityErrorNotifierService admissibilityErrorNotifierServiceMock;
    @Mock
    private OnboardingContextHolderService onboardingContextHolderServiceMock;
    @Mock
    private DeleteInitiativeService deleteInitiativeServiceMock;
    private CommandMediatorServiceImpl commandMediatorService;
    private MemoryAppender memoryAppender;

    @BeforeEach
    void setUp() {
        commandMediatorService =
                new CommandMediatorServiceImpl(
                        "Application Name",
                        100L,
                        "PT1S",
                        deleteInitiativeServiceMock,
                        onboardingContextHolderServiceMock,
                        admissibilityErrorNotifierServiceMock,
                        TestUtils.objectMapper);

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("it.gov.pagopa.admissibility.service.commands.CommandMediatorServiceImpl");
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }

    @Test
    void getCommitDelay() {
        //given
        Duration expected = Duration.ofMillis(100L);
        //when
        Duration commitDelay = commandMediatorService.getCommitDelay();
        //then
        Assertions.assertEquals(expected,commitDelay);
    }

    @Test
    void givenMessagesWhenAfterCommitsThenSuccessfully() {
        //given
        Flux<List<String>> afterCommits2Subscribe = Flux.just(List.of("INITIATIVE1","INITIATIVE2","INITIATIVE3"));
        Mockito.when(onboardingContextHolderServiceMock.refreshKieContainerCacheMiss()).thenReturn(Mono.just(Mockito.mock(KieBase.class)));

        // when
        commandMediatorService.subscribeAfterCommits(afterCommits2Subscribe);

        //then
        Mockito.verify(onboardingContextHolderServiceMock).refreshKieContainerCacheMiss();
        Assertions.assertEquals(
                ("[ADMISSIBILITY_COMMANDS] Processed offsets committed successfully"),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void getObjectReader() {
        ObjectReader objectReader = commandMediatorService.getObjectReader();
        Assertions.assertNotNull(objectReader);
    }

    @Test
    void givenDeleteInitiatveOperationTypeWhenCallExecuteThenReturnString() {
        //given
        QueueCommandOperationDTO payload = QueueCommandOperationDTO.builder()
                .entityId("DUMMY_INITITATIVEID")
                .operationTime(LocalDateTime.now())
                .operationType(CommandConstants.OPERATION_TYPE_DELETE_INITIATIVE)
                .build();

        Message<String> message = MessageBuilder.withPayload("INITIATIVE").setHeader("HEADER","DUMMY_HEADER").build();
        Map<String, Object> ctx = new HashMap<>();

        Mockito.when(deleteInitiativeServiceMock.execute(payload.getEntityId())).thenReturn(Mono.just(anyString()));

        //when
        String result = commandMediatorService.execute(payload, message, ctx).block();

        //then
        Assertions.assertNotNull(result);
        Mockito.verify(deleteInitiativeServiceMock).execute(anyString());
    }

    @Test
    void givenOperationTypeDifferentWhenCallExecuteThenReturnMonoEmpty(){
        //given
        QueueCommandOperationDTO payload = QueueCommandOperationDTO.builder()
                .entityId("DUMMY_INITITATIVEID")
                .operationTime(LocalDateTime.now())
                .operationType("OTHER_OPERATION_TYPE")
                .build();

        Message<String> message = MessageBuilder.withPayload("INITIATIVE").setHeader("HEADER","DUMMY_HEADER").build();
        Map<String, Object> ctx = new HashMap<>();
        //when
        Mono<String> result= commandMediatorService.execute(payload, message, ctx);

        //then
        assertEquals(result,Mono.empty());
        Mockito.verify(deleteInitiativeServiceMock,Mockito.never()).execute(anyString());
    }
    @Test
    void getFlowName() {
        //given
        String expected = "ADMISSIBILITY_COMMANDS";
        //when
        String result = commandMediatorService.getFlowName();
        //then
        Assertions.assertEquals(expected,result);
    }
}