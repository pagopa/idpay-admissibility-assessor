package it.gov.pagopa.admissibility.service.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.admissibility.dto.commands.QueueCommandOperationDTO;
import it.gov.pagopa.admissibility.service.AdmissibilityErrorNotifierService;
import it.gov.pagopa.admissibility.service.commands.operations.DeleteInitiativeService;
import it.gov.pagopa.admissibility.utils.CommandConstants;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
@Service
@Slf4j
public class CommandMediatorServiceImpl extends BaseKafkaConsumer<QueueCommandOperationDTO, String> implements CommandMediatorService{

    private final Duration commitDelay;
    private final DeleteInitiativeService deleteInitiativeService;
    private final AdmissibilityErrorNotifierService admissibilityErrorNotifierService;
    private final ObjectReader objectReader;

    public CommandMediatorServiceImpl(@Value("${spring.application.name}") String applicationName,
                                      @Value("${spring.cloud.stream.kafka.bindings.consumerCommands-in-0.consumer.ackTime}")  long commitMillis,
                                      DeleteInitiativeService deleteInitiativeService,
                                      AdmissibilityErrorNotifierService admissibilityErrorNotifierService,
                                      ObjectMapper objectMapper) {
        super(applicationName);
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.deleteInitiativeService = deleteInitiativeService;
        this.admissibilityErrorNotifierService = admissibilityErrorNotifierService;
        this.objectReader = objectMapper.readerFor(QueueCommandOperationDTO.class);
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<String>> afterCommits2subscribe) {
        afterCommits2subscribe
                .subscribe(r -> log.info("[ADMISSIBILITY_COMMANDS] Processed offsets committed successfully"));
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> admissibilityErrorNotifierService.notifyAdmissibilityCommands(message, "[ADMISSIBILITY_COMMANDS] Unexpected JSON", false, e);
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        admissibilityErrorNotifierService.notifyAdmissibilityCommands(message, "[ADMISSIBILITY_COMMANDS] An error occurred evaluating commands", true, e);
    }

    @Override
    protected Mono<String> execute(QueueCommandOperationDTO payload, Message<String> message, Map<String, Object> ctx) {
        if (CommandConstants.OPERATION_TYPE_DELETE_INITIATIVE.equals(payload.getOperationType())) {
            return deleteInitiativeService.execute(payload.getEntityId());
        }
        log.debug("[ADMISSIBILITY_COMMANDS] Invalid operation type {}", payload.getOperationType());
        return Mono.empty();
    }

    @Override
    public String getFlowName() {
        return "ADMISSIBILITY_COMMANDS";
    }
}
