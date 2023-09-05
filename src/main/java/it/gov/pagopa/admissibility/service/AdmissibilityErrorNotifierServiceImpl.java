package it.gov.pagopa.admissibility.service;

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

    private final ErrorNotifierService errorNotifierService;

    private final String beneficiaryRuleBuilderMessagingServiceType;
    private final String beneficiaryRuleBuilderServer;
    private final String beneficiaryRuleBuilderTopic;
    private final String beneficiaryRuleBuilderGroup;

    private final String admissibilityMessagingServiceType;
    private final String admissibilityServer;
    private final String admissibilityTopic;
    private final String admissibilityGroup;

    private final String admissibilityOutServiceType;
    private final String admissibilityOutServer;
    private final String admissibilityOutTopic;

    private final String admissibilityRankingRequestServiceType;
    private final String admissibilityRankingRequestServer;
    private final String admissibilityRankingRequestTopic;

    private final String admissibilityCommandServiceType;
    private final String admissibilityCommandServer;
    private final String admissibilityCommandTopic;
    private final String admissibilityCommandGroup;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public AdmissibilityErrorNotifierServiceImpl(ErrorNotifierService errorNotifierService,

                                                 @Value("${spring.cloud.stream.binders.kafka-beneficiary-rule-builder.type}") String beneficiaryRuleBuilderMessagingServiceType,
                                                 @Value("${spring.cloud.stream.binders.kafka-beneficiary-rule-builder.environment.spring.cloud.stream.kafka.binder.brokers}") String beneficiaryRuleBuilderServer,
                                                 @Value("${spring.cloud.stream.bindings.beneficiaryRuleBuilderConsumer-in-0.destination}") String beneficiaryRuleBuilderTopic,
                                                 @Value("${spring.cloud.stream.bindings.beneficiaryRuleBuilderConsumer-in-0.group}") String beneficiaryRuleBuilderGroup,

                                                 @Value("${spring.cloud.stream.binders.kafka-onboarding-request.type}") String admissibilityMessagingServiceType,
                                                 @Value("${spring.cloud.azure.servicebus.connection-string}") String admissibilityServer,
                                                 @Value("${spring.cloud.stream.bindings.admissibilityProcessor-in-0.destination}") String admissibilityTopic,
                                                 @Value("") String admissibilityGroup,

                                                 @Value("${spring.cloud.stream.binders.kafka-onboarding-outcome.type}") String admissibilityOutServiceType,
                                                 @Value("${spring.cloud.stream.binders.kafka-onboarding-outcome.environment.spring.cloud.stream.kafka.binder.brokers}") String admissibilityOutServer,
                                                 @Value("${spring.cloud.stream.bindings.admissibilityProcessorOut-out-0.destination}") String admissibilityOutTopic,

                                                 @Value("${spring.cloud.stream.binders.kafka-ranking-request.type}") String admissibilityRankingRequestServiceType,
                                                 @Value("${spring.cloud.stream.binders.kafka-ranking-request.environment.spring.cloud.stream.kafka.binder.brokers}") String admissibilityRankingRequestServer,
                                                 @Value("${spring.cloud.stream.bindings.rankingRequest-out-0.destination}") String admissibilityRankingRequestTopic,

                                                 @Value("${spring.cloud.stream.binders.kafka-commands.type}") String admissibilityCommandServiceType,
                                                 @Value("${spring.cloud.stream.binders.kafka-commands.environment.spring.cloud.stream.kafka.binder.brokers}") String admissibilityCommandServer,
                                                 @Value("${spring.cloud.stream.bindings.consumerCommands-in-0.destination}") String admissibilityCommandTopic,
                                                 @Value("${spring.cloud.stream.bindings.consumerCommands-in-0.group}") String admissibilityCommandGroup ) {
        this.errorNotifierService = errorNotifierService;

        this.beneficiaryRuleBuilderMessagingServiceType = beneficiaryRuleBuilderMessagingServiceType;
        this.beneficiaryRuleBuilderServer = beneficiaryRuleBuilderServer;
        this.beneficiaryRuleBuilderTopic = beneficiaryRuleBuilderTopic;
        this.beneficiaryRuleBuilderGroup = beneficiaryRuleBuilderGroup;

        this.admissibilityMessagingServiceType = admissibilityMessagingServiceType;
        this.admissibilityServer = extractServerFromServiceBusConnectionString(admissibilityServer);
        this.admissibilityTopic = admissibilityTopic;
        this.admissibilityGroup = admissibilityGroup;

        this.admissibilityOutServiceType = admissibilityOutServiceType;
        this.admissibilityOutServer = admissibilityOutServer;
        this.admissibilityOutTopic = admissibilityOutTopic;

        this.admissibilityRankingRequestServiceType = admissibilityRankingRequestServiceType;
        this.admissibilityRankingRequestServer = admissibilityRankingRequestServer;
        this.admissibilityRankingRequestTopic = admissibilityRankingRequestTopic;

        this.admissibilityCommandServiceType = admissibilityCommandServiceType;
        this.admissibilityCommandServer = admissibilityCommandServer;
        this.admissibilityCommandTopic = admissibilityCommandTopic;
        this.admissibilityCommandGroup = admissibilityCommandGroup;
    }

    private final Pattern serviceBusEndpointPattern = Pattern.compile("Endpoint=sb://([^;]+)/?;");
    private String extractServerFromServiceBusConnectionString(String connectionString){
        final Matcher matcher = serviceBusEndpointPattern.matcher(connectionString);
        return matcher.find() ? matcher.group(1) : "ServiceBus";
    }

    @Override
    public void notifyBeneficiaryRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(beneficiaryRuleBuilderMessagingServiceType, beneficiaryRuleBuilderServer, beneficiaryRuleBuilderTopic, beneficiaryRuleBuilderGroup, message, description, retryable, true,exception);
    }

    @Override
    public void notifyAdmissibility(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(admissibilityMessagingServiceType, admissibilityServer, admissibilityTopic, admissibilityGroup, message, description, retryable, true, exception);
    }

    @Override
    public void notifyAdmissibilityOutcome(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(admissibilityOutServiceType, admissibilityOutServer, admissibilityOutTopic, null, message, description, retryable, false, exception);
    }

    @Override
    public void notifyRankingRequest(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(admissibilityRankingRequestServiceType, admissibilityRankingRequestServer, admissibilityRankingRequestTopic,null, message, description, retryable, false, exception);
    }

    @Override
    public void notifyAdmissibilityCommands(Message<String> message, String description, boolean retryable, Throwable exception) {
        notify(admissibilityCommandServiceType, admissibilityCommandServer, admissibilityCommandTopic, admissibilityCommandGroup, message, description, retryable, true, exception);
    }

    @Override
    public void notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable,boolean resendApplication, Throwable exception) {
        errorNotifierService.notify(srcType, srcServer, srcTopic, group, message, description, retryable,resendApplication, exception);
    }

}
