package it.gov.pagopa.admissibility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import it.gov.pagopa.admissibility.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.admissibility.connector.repository.InitiativeCountersRepository;
import it.gov.pagopa.admissibility.connector.soap.inps.service.IseeConsultationSoapClient;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.common.kafka.KafkaTestUtilitiesService;
import it.gov.pagopa.common.mongo.MongoTestUtilitiesService;
import it.gov.pagopa.common.mongo.singleinstance.AutoConfigureSingleInstanceMongodb;
import it.gov.pagopa.common.rest.utils.WireMockUtils;
import it.gov.pagopa.common.stream.StreamsHealthIndicator;
import it.gov.pagopa.common.utils.JUnitExtensionContextHolder;
import it.gov.pagopa.common.utils.TestIntegrationUtils;
import it.gov.pagopa.common.utils.TestUtils;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.util.Pair;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ExtendWith(JUnitExtensionContextHolder.class)
@SpringBootTest
@EnableAutoConfiguration
@EmbeddedKafka(topics = {
        "${spring.cloud.stream.bindings.beneficiaryRuleBuilderConsumer-in-0.destination}",
        "${spring.cloud.stream.bindings.admissibilityProcessor-in-0.destination}",
        "${spring.cloud.stream.bindings.admissibilityProcessorOut-out-0.destination}",
        "${spring.cloud.stream.bindings.rankingRequest-out-0.destination}",
        "${spring.cloud.stream.bindings.errors-out-0.destination}",
        "${spring.cloud.stream.bindings.consumerCommands-in-0.destination}",
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
                "spring.cloud.stream.binders.kafka-commands.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                //endregion

                //region service bus
                // mocked replacing it using kafka
                "spring.cloud.azure.servicebus.connection-string=Endpoint=sb://ServiceBusEndpoint;SharedAccessKeyName=sharedAccessKeyName;SharedAccessKey=sharedAccessKey;EntityPath=entityPath",
                "spring.cloud.stream.binders.kafka-onboarding-request.type=kafka",
                "spring.cloud.stream.binders.kafka-onboarding-request.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.bindings.admissibilityDelayProducer-out-0.content-type=application/json",
                "spring.cloud.stream.kafka.bindings.admissibilityDelayProducer-out-0.producer.configuration.linger.ms=2",
                //endregion

                //region mongodb
                "logging.level.org.mongodb.driver=WARN",
                "logging.level.de.flapdoodle.embed.mongo.spring.autoconfigure=WARN",
                "de.flapdoodle.mongodb.embedded.version=4.2.24",
                //endregion

                //region pdv
                "app.pdv.retry.delay-millis=5000",
                "app.pdv.retry.max-attempts=3",
                //endregion
        })
@AutoConfigureSingleInstanceMongodb
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
    @Value("${spring.cloud.stream.bindings.consumerCommands-in-0.destination}")
    protected String topicCommands;

    @Value("${spring.cloud.stream.bindings.beneficiaryRuleBuilderConsumer-in-0.group}")
    protected String groupIdBeneficiaryRuleConsumer;
    @Value("${spring.cloud.stream.bindings.consumerCommands-in-0.group}")
    protected String groupIdCommands;

    @Value("${spring.data.redis.url}")
    protected String redisUrl;

    @BeforeAll
    public static void unregisterPreviouslyKafkaServers() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        TestIntegrationUtils.setDefaultTimeZoneAndUnregisterCommonMBean();
    }

    @PostConstruct
    public void logEmbeddedServerConfig() {
        String wiremockHttpBaseUrl = "UNKNOWN";
        String wiremockHttpsBaseUrl = "UNKNOWN";
        try {
            wiremockHttpBaseUrl = serverWireMockExtension.getRuntimeInfo().getHttpBaseUrl();
            wiremockHttpsBaseUrl = serverWireMockExtension.getRuntimeInfo().getHttpsBaseUrl();
        } catch (Exception e) {
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

    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"initiativeId\":\"INITIATIVEID_([0-9]+)_?[^\"]*\"");
    }

    protected void checkErrorsPublished(int expectedErrorMessagesNumber, long maxWaitingMs, List<Pair<Supplier<String>, java.util.function.Consumer<ConsumerRecord<String, String>>>> errorUseCases) {
        kafkaTestUtilitiesService.checkErrorsPublished(topicErrors, getErrorUseCaseIdPatternMatch(), expectedErrorMessagesNumber, maxWaitingMs, errorUseCases);
    }

    protected void checkErrorMessageHeaders(String srcTopic, String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        kafkaTestUtilitiesService.checkErrorMessageHeaders(kafkaBootstrapServers, srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, this::normalizePayload);
    }

    protected void checkErrorMessageHeaders(String server, String srcTopic, String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        kafkaTestUtilitiesService.checkErrorMessageHeaders(server, srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, this::normalizePayload);
    }

    protected void checkErrorMessageHeaders(String srcServer, String srcTopic, String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey, boolean expectRetryHeader, boolean expectedAppNameHeader) {
        kafkaTestUtilitiesService.checkErrorMessageHeaders(srcServer, srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, expectRetryHeader, expectedAppNameHeader, this::normalizePayload);
    }

    protected String normalizePayload(String expectedPayload) {
        return TestUtils.truncateDateTimeField(expectedPayload, "admissibilityCheckDate");
    }

    //region desc=Setting WireMock
    private static boolean WIREMOCK_REQUEST_CLIENT_AUTH = true;
    private static boolean USE_TRUSTORE_OK = true;
    private static final String TRUSTSTORE_PATH = "src/test/resources/wiremockKeyStore.p12";
    private static final String TRUSTSTORE_KO_PATH = "src/test/resources/wiremockTrustStoreKO.p12";
    @RegisterExtension
    static WireMockExtension serverWireMockExtension = initServerWiremock();

    public static void configureServerWiremockBeforeAll(boolean needClientAuth, boolean useTrustoreOk) {
        WIREMOCK_REQUEST_CLIENT_AUTH = needClientAuth;
        USE_TRUSTORE_OK = useTrustoreOk;
        initServerWiremock();
    }

    private static WireMockExtension initServerWiremock() {
        int httpPort=0;
        int httpsPort=0;
        boolean start=false;

        // re-using shutdown server port in order to let Spring loaded configuration still valid
        if (serverWireMockExtension != null && JUnitExtensionContextHolder.extensionContext != null) {
            try {
                httpPort = serverWireMockExtension.getRuntimeInfo().getHttpPort();
                httpsPort = serverWireMockExtension.getRuntimeInfo().getHttpsPort();

                serverWireMockExtension.shutdownServer();
                // waiting server stop, releasing ports
                TestUtils.wait(200, TimeUnit.MILLISECONDS);
                start=true;
            } catch (IllegalStateException e){
                // Do Nothing: the wiremock server was not started
            }
        }

        WireMockExtension newWireMockConfig = WireMockUtils.initServerWiremock(
                httpPort,
                httpsPort,
                "src/test/resources/stub",
                WIREMOCK_REQUEST_CLIENT_AUTH,
                USE_TRUSTORE_OK ? TRUSTSTORE_PATH : TRUSTSTORE_KO_PATH,
                "idpay");

        if(start){
            try {
                newWireMockConfig.beforeAll(JUnitExtensionContextHolder.extensionContext);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot start WireMock JUnit Extension", e);
            }
        }

        return serverWireMockExtension = newWireMockConfig;
    }

    @AfterAll
    static void restoreWireMockConfig() {
        if(!USE_TRUSTORE_OK || !WIREMOCK_REQUEST_CLIENT_AUTH) {
            USE_TRUSTORE_OK = true;
            WIREMOCK_REQUEST_CLIENT_AUTH = true;
            initServerWiremock();
        }
    }

    public static class WireMockInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
            // setting wiremock HTTP baseUrl
            Stream.of(
                    Pair.of("app.pdv.base-url", "pdv"),
                    Pair.of("app.pdnd.base-url", "pdnd"),
                    Pair.of("app.idpay-mock.base-url", "pdndMock") //TODO removeme once integrated real system
            ).forEach(setWireMockBaseMockedServicePath(applicationContext, serverWireMockExtension.getRuntimeInfo().getHttpBaseUrl()));

            // setting wiremock HTTPS baseUrl
            Stream.of(
                    Pair.of("app.anpr.config.base-url", "anpr/"),
                    Pair.of("app.inps.iseeConsultation.base-url", "inps/isee")
            ).forEach(setWireMockBaseMockedServicePath(applicationContext, serverWireMockExtension.getRuntimeInfo().getHttpsBaseUrl()));

            System.out.printf("""
                            ************************
                            Server wiremock:
                            http base url: %s
                            https base url: %s
                            ************************
                            """,
                    serverWireMockExtension.getRuntimeInfo().getHttpBaseUrl(),
                    serverWireMockExtension.getRuntimeInfo().getHttpsBaseUrl());
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
    void initWiremockEqualToXmlPattern() {
        try {
            iseeConsultationSoapClient.getIsee("CF_OK", IseeTypologyEnum.ORDINARIO).block();
        } catch (Exception e) {
            //Do Nothing
        }
    }
//endregion
}
