package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.config.KafkaConfiguration;
import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AdmissibilityErrorNotifierServiceImpl implements AdmissibilityErrorNotifierService {

    public static final String KAFKA_BINDINGS_BENEFICIARY_RULE_BUILDER = "beneficiaryRuleBuilderConsumer-in-0";
    private static final String KAFKA_BINDERS_ADMISSIBILITY = "kafka-onboarding-request";
    private static final String KAFKA_BINDINGS_ADMISSIBILITY = "admissibilityProcessor-in-0";
    private static final String KAFKA_BINDINGS_ADMISSIBILITY_OUT = "admissibilityProcessorOut-out-0";
    private static final String KAFKA_BINDINGS_ADMISSIBILITY_RANKING_REQUEST = "rankingRequest-out-0";
    private static final String KAFKA_BINDINGS_ADMISSIBILITY_COMMANDS = "consumerCommands-in-0";

    private final ErrorNotifierService errorNotifierService;
    private final String admissibilityServer;
    private final KafkaConfiguration kafkaConfiguration;

    public AdmissibilityErrorNotifierServiceImpl(ErrorNotifierService errorNotifierService,
                                                 KafkaConfiguration kafkaConfiguration,
                                                 @Value("${spring.cloud.azure.servicebus.connection-string}") String admissibilityServer) {
        this.errorNotifierService = errorNotifierService;
        this.kafkaConfiguration = kafkaConfiguration;
        this.admissibilityServer = extractServerFromServiceBusConnectionString(admissibilityServer);
    }

    private final Pattern serviceBusEndpointPattern = Pattern.compile("Endpoint=sb://([^;]+)/?;");
    private String extractServerFromServiceBusConnectionString(String connectionString){
        final Matcher matcher = serviceBusEndpointPattern.matcher(connectionString);
        return matcher.find() ? matcher.group(1) : "ServiceBus";
    }

    @Override
    public void notifyBeneficiaryRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(kafkaConfiguration.getStream().getBindings().get(KAFKA_BINDINGS_BENEFICIARY_RULE_BUILDER), message, description, retryable, true,exception);
    }

    @Override
    public void notifyAdmissibility(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(new KafkaConfiguration.BaseKafkaInfoDTO(
                kafkaConfiguration.getTopicForBindings(KAFKA_BINDINGS_ADMISSIBILITY),
                "",
                kafkaConfiguration.getTypeForBinder(KAFKA_BINDERS_ADMISSIBILITY),
                admissibilityServer), message, description, retryable, true, exception);
    }

    @Override
    public void notifyAdmissibilityOutcome(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(kafkaConfiguration.getStream().getBindings().get(KAFKA_BINDINGS_ADMISSIBILITY_OUT), message, description, retryable, false, exception);
    }

    @Override
    public void notifyRankingRequest(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(kafkaConfiguration.getStream().getBindings().get(KAFKA_BINDINGS_ADMISSIBILITY_RANKING_REQUEST), message, description, retryable, false, exception);
    }

    @Override
    public void notifyAdmissibilityCommands(Message<String> message, String description, boolean retryable, Throwable exception) {
        notify(kafkaConfiguration.getStream().getBindings().get(KAFKA_BINDINGS_ADMISSIBILITY_COMMANDS), message, description, retryable, true, exception);
    }

    @Override
    public void notify(KafkaConfiguration.BaseKafkaInfoDTO basekafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception) {
        errorNotifierService.notify(basekafkaInfoDTO, message, description, retryable,resendApplication, exception);
    }

}
