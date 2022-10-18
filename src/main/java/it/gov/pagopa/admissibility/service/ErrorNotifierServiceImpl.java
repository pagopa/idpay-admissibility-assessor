package it.gov.pagopa.admissibility.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ErrorNotifierServiceImpl implements ErrorNotifierService {

    public static final String ERROR_MSG_HEADER_APPLICATION_NAME = "applicationName";
    public static final String ERROR_MSG_HEADER_GROUP = "group";
    public static final String ERROR_MSG_HEADER_SRC_TYPE = "srcType";
    public static final String ERROR_MSG_HEADER_SRC_SERVER = "srcServer";
    public static final String ERROR_MSG_HEADER_SRC_TOPIC = "srcTopic";
    public static final String ERROR_MSG_HEADER_DESCRIPTION = "description";
    public static final String ERROR_MSG_HEADER_RETRYABLE = "retryable";
    public static final String ERROR_MSG_HEADER_STACKTRACE = "stacktrace";

    private final StreamBridge streamBridge;
    private final String applicationName;

    private final String beneficiaryRuleBuilderMessagingServiceType;
    private final String beneficiaryRuleBuilderServer;
    private final String beneficiaryRuleBuilderTopic;
    private final String beneficiaryRuleBuilderGroup;

    private final String admissibilityMessagingServiceType;
    private final String admissibilityServer;
    private final String admissibilityTopic;
    private final String admissibilityGroup;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public ErrorNotifierServiceImpl(StreamBridge streamBridge,
                                    @Value("${spring.application.name}") String applicationName,

                                    @Value("${spring.cloud.stream.binders.kafka-beneficiary-rule-builder.type}") String beneficiaryRuleBuilderMessagingServiceType,
                                    @Value("${spring.cloud.stream.binders.kafka-beneficiary-rule-builder.environment.spring.cloud.stream.kafka.binder.brokers}") String beneficiaryRuleBuilderServer,
                                    @Value("${spring.cloud.stream.bindings.beneficiaryRuleBuilderConsumer-in-0.destination}") String beneficiaryRuleBuilderTopic,
                                    @Value("${spring.cloud.stream.bindings.beneficiaryRuleBuilderConsumer-in-0.group}") String beneficiaryRuleBuilderGroup,

                                    @Value("${spring.cloud.stream.binders.kafka-onboarding-request.type}") String admissibilityMessagingServiceType,
                                    @Value("${spring.cloud.azure.servicebus.connection-string}") String admissibilityServer,
                                    @Value("${spring.cloud.stream.bindings.admissibilityProcessor-in-0.destination}") String admissibilityTopic,
                                    @Value("") String admissibilityGroup
                                    ) {
        this.streamBridge = streamBridge;
        this.applicationName = applicationName;

        this.beneficiaryRuleBuilderMessagingServiceType = beneficiaryRuleBuilderMessagingServiceType;
        this.beneficiaryRuleBuilderServer = beneficiaryRuleBuilderServer;
        this.beneficiaryRuleBuilderTopic = beneficiaryRuleBuilderTopic;
        this.beneficiaryRuleBuilderGroup = beneficiaryRuleBuilderGroup;

        this.admissibilityMessagingServiceType = admissibilityMessagingServiceType;
        this.admissibilityServer = extractServerFromServiceBusConnectionString(admissibilityServer);
        this.admissibilityTopic = admissibilityTopic;
        this.admissibilityGroup = admissibilityGroup;
    }

    private final Pattern serviceBusEndpointPattern = Pattern.compile("Endpoint=sb://([^;]+)/?;");
    private String extractServerFromServiceBusConnectionString(String connectionString){
        final Matcher matcher = serviceBusEndpointPattern.matcher(connectionString);
        return matcher.find() ? matcher.group(1) : "ServiceBus";
    }

    @Override
    public void notifyBeneficiaryRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(beneficiaryRuleBuilderMessagingServiceType, beneficiaryRuleBuilderServer, beneficiaryRuleBuilderTopic, beneficiaryRuleBuilderGroup, message, description, retryable, exception);
    }

    @Override
    public void notifyAdmissibility(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(admissibilityMessagingServiceType, admissibilityServer, admissibilityTopic, admissibilityGroup, message, description, retryable, exception);
    }

    @Override
    public void notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable, Throwable exception) {
        log.info("[ERROR_NOTIFIER] notifying error: {}", description, exception);
        final MessageBuilder<?> errorMessage = MessageBuilder.fromMessage(message)
                .setHeader(ERROR_MSG_HEADER_APPLICATION_NAME, applicationName)
                .setHeader(ERROR_MSG_HEADER_GROUP, group)
                .setHeader(ERROR_MSG_HEADER_SRC_TYPE, srcType)
                .setHeader(ERROR_MSG_HEADER_SRC_SERVER, srcServer)
                .setHeader(ERROR_MSG_HEADER_SRC_TOPIC, srcTopic)
                .setHeader(ERROR_MSG_HEADER_DESCRIPTION, description)
                .setHeader(ERROR_MSG_HEADER_RETRYABLE, retryable)
                .setHeader(ERROR_MSG_HEADER_STACKTRACE, ExceptionUtils.getStackTrace(exception));

        addExceptionInfo(errorMessage, "rootCause", ExceptionUtils.getRootCause(exception));
        addExceptionInfo(errorMessage, "cause", exception.getCause());

        byte[] receivedKey = message.getHeaders().get(KafkaHeaders.RECEIVED_MESSAGE_KEY, byte[].class);
        if(receivedKey!=null){
            errorMessage.setHeader(KafkaHeaders.MESSAGE_KEY, new String(receivedKey, StandardCharsets.UTF_8));
        }

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
