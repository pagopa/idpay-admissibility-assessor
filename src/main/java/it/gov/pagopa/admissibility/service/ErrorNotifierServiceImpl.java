package it.gov.pagopa.admissibility.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ErrorNotifierServiceImpl implements ErrorNotifierService {

    public static final String ERROR_MSG_HEADER_SRC_TYPE = "srcType";
    public static final String ERROR_MSG_HEADER_SRC_SERVER = "srcServer";
    public static final String ERROR_MSG_HEADER_SRC_TOPIC = "srcTopic";
    public static final String ERROR_MSG_HEADER_DESCRIPTION = "description";
    public static final String ERROR_MSG_HEADER_RETRYABLE = "retryable";
    public static final String ERROR_MSG_HEADER_STACKTRACE = "stacktrace";

    private final StreamBridge streamBridge;

    private final String beneficiaryRuleBuilderMessagingServiceType;
    private final String beneficiaryRuleBuilderServer;
    private final String beneficiaryRuleBuilderTopic;

    private final String admissibilityMessagingServiceType;
    private final String admissibilityServer;
    private final String admissibilityTopic;

    public ErrorNotifierServiceImpl(StreamBridge streamBridge,

                                    @Value("${spring.cloud.stream.binders.kafka-beneficiary-rule-builder.type}") String beneficiaryRuleBuilderMessagingServiceType,
                                    @Value("${spring.cloud.stream.binders.kafka-beneficiary-rule-builder.environment.spring.cloud.stream.kafka.binder.brokers}") String beneficiaryRuleBuilderServer,
                                    @Value("${spring.cloud.stream.bindings.beneficiaryRuleBuilderConsumer-in-0.destination}") String beneficiaryRuleBuilderTopic,

                                    @Value("${spring.cloud.stream.binders.kafka-onboarding-request.type}") String admissibilityMessagingServiceType,
                                    @Value("${spring.cloud.azure.servicebus.connection-string}") String admissibilityServer,
                                    @Value("${spring.cloud.stream.bindings.admissibilityProcessor-in-0.destination}") String admissibilityTopic) {
        this.streamBridge = streamBridge;

        this.beneficiaryRuleBuilderMessagingServiceType = beneficiaryRuleBuilderMessagingServiceType;
        this.beneficiaryRuleBuilderServer = beneficiaryRuleBuilderServer;
        this.beneficiaryRuleBuilderTopic = beneficiaryRuleBuilderTopic;

        this.admissibilityMessagingServiceType = admissibilityMessagingServiceType;
        this.admissibilityServer = extractServerFromServiceBusConnectionString(admissibilityServer);
        this.admissibilityTopic = admissibilityTopic;
    }

    private final Pattern serviceBusEndpointPattern = Pattern.compile("Endpoint=sb://([^;]+)/?;");
    private String extractServerFromServiceBusConnectionString(String connectionString){
        final Matcher matcher = serviceBusEndpointPattern.matcher(connectionString);
        return matcher.find() ? matcher.group(1) : "ServiceBus";
    }

    @Override
    public void notifyBeneficiaryRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(beneficiaryRuleBuilderMessagingServiceType, beneficiaryRuleBuilderServer, beneficiaryRuleBuilderTopic, message, description, retryable, exception);
    }

    @Override
    public void notifyAdmissibility(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(admissibilityMessagingServiceType, admissibilityServer, admissibilityTopic, message, description, retryable, exception);
    }

    @Override
    public void notify(String srcType, String srcServer, String srcTopic, Message<?> message, String description, boolean retryable, Throwable exception) {
        log.info("[ERROR_NOTIFIER] notifying error: {}", description, exception);
        final MessageBuilder<?> errorMessage = MessageBuilder.fromMessage(message)
                .setHeader(ERROR_MSG_HEADER_SRC_TYPE, srcType)
                .setHeader(ERROR_MSG_HEADER_SRC_SERVER, srcServer)
                .setHeader(ERROR_MSG_HEADER_SRC_TOPIC, srcTopic)
                .setHeader(ERROR_MSG_HEADER_DESCRIPTION, description)
                .setHeader(ERROR_MSG_HEADER_RETRYABLE, retryable)
                .setHeader(ERROR_MSG_HEADER_STACKTRACE, ExceptionUtils.getStackTrace(exception));

        addExceptionInfo(errorMessage, "rootCause", ExceptionUtils.getRootCause(exception));
        addExceptionInfo(errorMessage, "cause", exception.getCause());

        if (!streamBridge.send("errors-out-0", errorMessage.build())) {
            log.error("[ERROR_NOTIFIER] Something gone wrong while notifying error");
        }
    }

    private void addExceptionInfo(MessageBuilder<?> errorMessage, String exceptionHeaderPrefix, Throwable rootCause) {
        errorMessage
                .setHeader("%sClass".formatted(exceptionHeaderPrefix), rootCause != null ? rootCause.getClass().getName() : null)
                .setHeader("%sMessage".formatted(exceptionHeaderPrefix), rootCause != null ? rootCause.getMessage() : null);
    }
}
