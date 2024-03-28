package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.config.KafkaConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@TestPropertySource( properties = {
        "spring.cloud.stream.binders.binder-test.type=kafka",
        "spring.cloud.stream.binders.binder-test.environment.spring.cloud.stream.kafka.binder.brokers=BROKERTEST",
        "spring.cloud.stream.bindings.binding-test-in-0.destination=topic_test",
        "spring.cloud.stream.bindings.binding-test-in-0.binder=binder-test",
        "spring.cloud.stream.bindings.binding-test-in-0.group=group-test",

        "spring.cloud.stream.bindings.binding-test-without-binder-in-0.destination=topic_test-without-binder",
        "spring.cloud.stream.bindings.binding-test-without-binder-in-0.binder=unexpected-binder",

        "spring.cloud.stream.binders.binder-test-without-environment.type=kafka",
        "spring.cloud.stream.bindings.binding-test-without-environment-in-0.destination=topic_test-without-environment",
        "spring.cloud.stream.bindings.binding-test-without-environment-in-0.binder=binder-test-without-environment"
})
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = KafkaConfiguration.class)
class KafkaConfigurationTest {

    @Value("${spring.cloud.stream.binders.binder-test.type}")
    private String binderTestType;
    @Value("${spring.cloud.stream.binders.binder-test.environment.spring.cloud.stream.kafka.binder.brokers}")
    private String binderTestBroker;
    @Value("${spring.cloud.stream.bindings.binding-test-in-0.destination}")
    private String binderTestTopic;
    @Value("${spring.cloud.stream.bindings.binding-test-in-0.binder}")
    private String binderTestName;
    @Value("${spring.cloud.stream.bindings.binding-test-in-0.group}")
    private String binderTestGroup;
    @Value("${spring.cloud.stream.bindings.binding-test-without-binder-in-0.destination}")
    private String withoutBinderTopic;
    @Value("${spring.cloud.stream.bindings.binding-test-without-binder-in-0.binder}")
    private String withoutBinderName;

    @Value("${spring.cloud.stream.binders.binder-test-without-environment.type}")
    private String withoutEnvironmentType;
    @Value("${spring.cloud.stream.bindings.binding-test-without-environment-in-0.destination}")
    private String withoutEnvironmentTopic;
    @Value("${spring.cloud.stream.bindings.binding-test-without-environment-in-0.binder}")
    private String withoutEnvironmentName;

    @Autowired
    private KafkaConfiguration config;


    @Test
    void getStream() {
        Map<String, KafkaConfiguration.KafkaInfoDTO> bindings = config.getStream().getBindings();
        assertEquals(3, bindings.size());

        KafkaConfiguration.KafkaInfoDTO kafkaInfoDTO = bindings.get("binding-test-in-0");
        Assertions.assertNotNull(kafkaInfoDTO);
        assertEquals(binderTestType, kafkaInfoDTO.getType());
        assertEquals(binderTestTopic, kafkaInfoDTO.getDestination());
        assertEquals(binderTestGroup, kafkaInfoDTO.getGroup());
        assertEquals(binderTestBroker, kafkaInfoDTO.getBrokers());
        assertEquals(binderTestName, kafkaInfoDTO.getBinder());

        Map<String, KafkaConfiguration.Binders> binders = config.getStream().getBinders();
        assertEquals(2, binders.size());
        KafkaConfiguration.Binders binderDTO = binders.get(binderTestName);
        assertEquals(binderTestType, binderDTO.getType());
        assertEquals(binderTestBroker, binderDTO.getEnvironment().getSpring().getCloud().getStream().getKafka().getBinder().getBrokers());
    }

    @Test
    void getStreamWithoutBinders() {
        Map<String, KafkaConfiguration.KafkaInfoDTO> bindings = config.getStream().getBindings();
        assertEquals(3, bindings.size());

        KafkaConfiguration.KafkaInfoDTO kafkaInfoDTO = bindings.get("binding-test-without-binder-in-0");
        Assertions.assertNotNull(kafkaInfoDTO);
        assertEquals(withoutBinderTopic, kafkaInfoDTO.getDestination());
        Assertions.assertNull(kafkaInfoDTO.getGroup());
        assertEquals(withoutBinderName,kafkaInfoDTO.getBinder());
        Assertions.assertNull(kafkaInfoDTO.getType());
        Assertions.assertNull(kafkaInfoDTO.getBrokers());

        Map<String, KafkaConfiguration.Binders> binders = config.getStream().getBinders();
        assertEquals(2, binders.size());
        KafkaConfiguration.Binders binderDTO = binders.get(withoutBinderName);
        Assertions.assertNull(binderDTO);
    }

    @Test
    void getStreamWithoutEnvironment() {
        Map<String, KafkaConfiguration.KafkaInfoDTO> bindings = config.getStream().getBindings();
        assertEquals(3, bindings.size());
        KafkaConfiguration.KafkaInfoDTO kafkaInfoDTO = bindings.get("binding-test-without-environment-in-0");
        Assertions.assertNotNull(kafkaInfoDTO);
        assertEquals(withoutEnvironmentTopic, kafkaInfoDTO.getDestination());
        Assertions.assertNull(kafkaInfoDTO.getGroup());
        assertEquals(withoutEnvironmentName, kafkaInfoDTO.getBinder());
        assertEquals(withoutEnvironmentType, kafkaInfoDTO.getType());
        Assertions.assertNull(kafkaInfoDTO.getBrokers());
        Map<String, KafkaConfiguration.Binders> binders = config.getStream().getBinders();
        assertEquals(2, binders.size());
        KafkaConfiguration.Binders binderDTO = binders.get(withoutEnvironmentName);
        assertEquals(withoutEnvironmentType, binderDTO.getType());
        Assertions.assertNull(binderDTO.getEnvironment());
    }

    @Test
    void getTopicForBindings(){
        Map<String, KafkaConfiguration.KafkaInfoDTO> bindings = config.getStream().getBindings();
        assertEquals(3, bindings.size());

        String topicForBindings = config.getTopicForBindings("binding-test-in-0");
        assertEquals(binderTestTopic, topicForBindings);

    }
    @Test
    void getTopicForBindingsWithoutKafkaInfoDTO(){
        Map<String, KafkaConfiguration.KafkaInfoDTO> bindings = config.getStream().getBindings();
        assertEquals(3, bindings.size());

        String topicForBindings = config.getTopicForBindings("unexpected-bindings");
        Assertions.assertNull(topicForBindings);

    }
    @Test
    void testEqualsAndHashCode() {
        KafkaConfiguration.BaseKafkaInfoDTO base1 = KafkaConfiguration.BaseKafkaInfoDTO.builder()
                .destination("destination1")
                .group("group1")
                .type("type1")
                .brokers("brokers1")
                .build();

        KafkaConfiguration.BaseKafkaInfoDTO base2 = KafkaConfiguration.BaseKafkaInfoDTO.builder()
                .destination("destination1")
                .group("group1")
                .type("type1")
                .brokers("brokers1")
                .build();

        KafkaConfiguration.KafkaInfoDTO kafka1 = KafkaConfiguration.KafkaInfoDTO.builder()
                .destination("destination1")
                .group("group1")
                .type("type1")
                .brokers("brokers1")
                .binder("binder1")
                .build();

        KafkaConfiguration.KafkaInfoDTO kafka2 = KafkaConfiguration.KafkaInfoDTO.builder()
                .destination("destination1")
                .group("group1")
                .type("type1")
                .brokers("brokers1")
                .binder("binder1")
                .build();


        assertEquals(base1, base2);
        assertEquals(kafka1, kafka2);


        assertNotEquals(base1, kafka1);


        assertEquals(base1.hashCode(), base2.hashCode());
        assertEquals(kafka1.hashCode(), kafka2.hashCode());


        assertNotEquals(base1.hashCode(), kafka1.hashCode());


    }

}