package it.gov.pagopa.admissibility;

import com.azure.spring.cloud.autoconfigure.kafka.AzureEventHubsKafkaOAuth2AutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.connector.soap.inps.IseeConsultationSoapClient;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.utils.RestTestUtils;
import it.gov.pagopa.common.kafka.KafkaTestUtilitiesService;
import it.gov.pagopa.common.mongo.MongoTestUtilitiesService;
import it.gov.pagopa.common.stream.StreamsHealthIndicator;
import it.gov.pagopa.common.utils.TestIntegrationUtils;
import it.gov.pagopa.common.utils.TestUtils;
import lombok.NonNull;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.util.Pair;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

import javax.annotation.PostConstruct;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@SpringBootTest
@EnableAutoConfiguration(exclude = AzureEventHubsKafkaOAuth2AutoConfiguration.class)
@EmbeddedKafka(topics = {
        "${spring.cloud.stream.bindings.beneficiaryRuleBuilderConsumer-in-0.destination}",
        "${spring.cloud.stream.bindings.admissibilityProcessor-in-0.destination}",
        "${spring.cloud.stream.bindings.admissibilityProcessorOut-out-0.destination}",
        "${spring.cloud.stream.bindings.rankingRequest-out-0.destination}",
        "${spring.cloud.stream.bindings.errors-out-0.destination}",
}, controlledShutdown = true)
@TestPropertySource(
        properties = {
                // even if enabled into application.yml, spring test will not load it https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.spring-boot-applications.jmx
//                "spring.jmx.enabled=true",

                //region common feature disabled
                "app.beneficiary-rule.cache.refresh-ms-rate:600000",
                "logging.level.it.gov.pagopa.common.kafka.service.ErrorNotifierServiceImpl=WARN",
                //endregion

                //region kafka brokers
                "logging.level.org.apache.zookeeper=WARN",
                "logging.level.org.apache.kafka=WARN",
                "logging.level.kafka=WARN",
                "logging.level.state.change.logger=WARN",
                "spring.cloud.stream.kafka.binder.configuration.security.protocol=PLAINTEXT",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.kafka.binder.zkNodes=${spring.embedded.zookeeper.connect}",
                "spring.cloud.stream.binders.kafka-beneficiary-rule-builder.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-onboarding-outcome.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-ranking-request.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-errors.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                //endregion

                //region service bus
                // mocked replacing it using kafka
                "spring.cloud.azure.servicebus.connection-string=Endpoint=sb://ServiceBusEndpoint;SharedAccessKeyName=sharedAccessKeyName;SharedAccessKey=sharedAccessKey;EntityPath=entityPath",
                "spring.cloud.stream.binders.kafka-onboarding-request.type=kafka",
                "spring.cloud.stream.binders.kafka-onboarding-request.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.bindings.admissibilityProcessor-in-0.destination=idpay-onboarding-request",
                //endregion

                //region mongodb
                "logging.level.org.mongodb.driver=WARN",
                "logging.level.org.springframework.boot.autoconfigure.mongo.embedded=WARN",
                "spring.mongodb.embedded.version=4.0.21",
                //endregion

                //region pdv
                "app.pdv.retry.delay-millis=5000",
                "app.pdv.retry.max-attempts=3",
                //endregion
        })
@AutoConfigureDataMongo
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = {BaseIntegrationTest.WireMockInitializer.class})
public abstract class BaseIntegrationTest {

    @Autowired
    protected KafkaTestUtilitiesService kafkaTestUtilitiesService;
    @Autowired
    protected MongoTestUtilitiesService mongoTestUtilitiesService;

    @Autowired
    protected DroolsRuleRepository droolsRuleRepository;
    @Autowired
    protected InitiativeCountersRepository initiativeCountersRepository;
    @Autowired
    protected StreamsHealthIndicator streamsHealthIndicator;
    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    protected String kafkaBootstrapServers;
    protected String serviceBusServers = "ServiceBusEndpoint";

    @Value("${spring.cloud.stream.bindings.beneficiaryRuleBuilderConsumer-in-0.destination}")
    protected String topicBeneficiaryRuleConsumer;
    @Value("${spring.cloud.stream.bindings.admissibilityProcessor-in-0.destination}")
    protected String topicAdmissibilityProcessorRequest;
    @Value("${spring.cloud.stream.bindings.admissibilityProcessorOut-out-0.destination}")
    protected String topicAdmissibilityProcessorOutcome;
    @Value("${spring.cloud.stream.bindings.rankingRequest-out-0.destination}")
    protected String topicAdmissibilityProcessorOutRankingRequest;
    @Value("${spring.cloud.stream.bindings.errors-out-0.destination}")
    protected String topicErrors;

    @Value("${spring.cloud.stream.bindings.beneficiaryRuleBuilderConsumer-in-0.group}")
    protected String groupIdBeneficiaryRuleConsumer;

    @Value("${spring.redis.url}")
    protected String redisUrl;

    @BeforeAll
    public static void unregisterPreviouslyKafkaServers() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        TestIntegrationUtils.setDefaultTimeZoneAndUnregisterCommonMBean();
    }

    @PostConstruct
    public void logEmbeddedServerConfig() {
        String wiremockHttpBaseUrl="UNKNOWN";
        String wiremockHttpsBaseUrl="UNKNOWN";
        try{
            wiremockHttpBaseUrl = serverWireMock.getRuntimeInfo().getHttpBaseUrl();
            wiremockHttpsBaseUrl = serverWireMock.getRuntimeInfo().getHttpsBaseUrl();
        } catch (Exception e){
            System.out.println("Cannot read wiremock urls");
        }
        System.out.printf("""
                        ************************
                        Embedded mongo: %s
                        Embedded kafka: %s
                        Embedded redis: %s
                        WireMock http: %s
                        WireMock https: %s
                        ************************
                        """,
                mongoTestUtilitiesService.getMongoUrl(),
                kafkaTestUtilitiesService.getKafkaUrls(),
                redisUrl,
                wiremockHttpBaseUrl,
                wiremockHttpsBaseUrl);
    }

    @Test
    void testHealthIndicator() {
        Health health = streamsHealthIndicator.health();
        Assertions.assertEquals(Status.UP, health.getStatus());
    }

    private final Pattern errorUseCaseIdPatternMatch = Pattern.compile("\"initiativeId\":\"id_([0-9]+)_?[^\"]*\"");

    protected void checkErrorsPublished(int expectedErrorMessagesNumber, long maxWaitingMs, List<Pair<Supplier<String>, java.util.function.Consumer<ConsumerRecord<String, String>>>> errorUseCases) {
        kafkaTestUtilitiesService.checkErrorsPublished(topicErrors, errorUseCaseIdPatternMatch, expectedErrorMessagesNumber, maxWaitingMs, errorUseCases);
    }

    protected void checkErrorMessageHeaders(String srcTopic,String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        kafkaTestUtilitiesService.checkErrorMessageHeaders(kafkaBootstrapServers, srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, this::normalizePayload);
    }

    protected void checkErrorMessageHeaders(String server, String srcTopic,String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        kafkaTestUtilitiesService.checkErrorMessageHeaders(server, srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, this::normalizePayload);
    }

    protected void checkErrorMessageHeaders(String srcServer, String srcTopic,String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey, boolean expectRetryHeader, boolean expectedAppNameHeader) {
        kafkaTestUtilitiesService.checkErrorMessageHeaders(srcServer, srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, expectRetryHeader, expectedAppNameHeader, this::normalizePayload);
    }

    protected String normalizePayload(String expectedPayload) {
        return TestUtils.truncateDateTimeField(expectedPayload, "admissibilityCheckDate");
    }

    //region desc=Setting WireMock
    @RegisterExtension
    static WireMockExtension serverWireMock = initServerWiremock();

    public static WireMockExtension initServerWiremock() {
        return serverWireMock = WireMockExtension.newInstance()
                .options(RestTestUtils.getWireMockConfiguration())
                .build();
    }

    public static class WireMockInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
            // setting wiremock HTTP baseUrl
            Stream.of(
                    Pair.of("app.pdv.base-url","pdv"),
                    Pair.of("app.pdnd.access.token-base-url","pdnd")
            ).forEach(setWireMockBaseMockedServicePath(applicationContext, serverWireMock.getRuntimeInfo().getHttpBaseUrl()));

            // setting wiremock HTTPS baseUrl
            Stream.of(
                    Pair.of("app.anpr.c020-residenceAssessment.base-url","anpr/residence"),
                            Pair.of("app.inps.iseeConsultation.base-url","inps/isee")
            ).forEach(setWireMockBaseMockedServicePath(applicationContext, serverWireMock.getRuntimeInfo().getHttpsBaseUrl()));

            System.out.printf("""
                            ************************
                            Server wiremock:
                            http base url: %s
                            https base url: %s
                            ************************
                            """,
                    serverWireMock.getRuntimeInfo().getHttpBaseUrl(),
                    serverWireMock.getRuntimeInfo().getHttpsBaseUrl());
        }

        private static java.util.function.Consumer<Pair<String, String>> setWireMockBaseMockedServicePath(ConfigurableApplicationContext applicationContext, String serverWireMock) {
            return key2basePath -> TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                    String.format("%s=%s/%s", key2basePath.getFirst(), serverWireMock, key2basePath.getSecond())
            );
        }
    }

    @Autowired
    private IseeConsultationSoapClient iseeConsultationSoapClient;

    /**
     * Due to a bug into concurrency handling of com.github.tomakehurst.wiremock.common.xml.Xml.read,
     * current com.github.tomakehurst.wiremock.matching.EqualToXmlPattern implementation require to pre-load stubs using such configuration
    */
    @PostConstruct
    void initWiremockEqualToXmlPattern(){
        try{
            iseeConsultationSoapClient.getIsee("CF_OK", IseeTypologyEnum.ORDINARIO).block();
        } catch (Exception e){
            //Do Nothing
        }
    }
//endregion
}
