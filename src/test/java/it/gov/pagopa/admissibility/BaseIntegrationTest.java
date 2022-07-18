package it.gov.pagopa.admissibility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.admissibility.repository.DroolsRuleRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@SpringBootTest
@EmbeddedKafka(topics = {
        "${spring.cloud.stream.bindings.beneficiaryRuleConsumer-in-0.destination}",
        "${spring.cloud.stream.bindings.admissibilityProcessor-in-0.destination}",
        "${spring.cloud.stream.bindings.admissibilityProcessor-out-0.destination}",
}, controlledShutdown = true)
@TestPropertySource(
        properties = {
                //region kafka brokers
                "logging.level.org.apache.zookeeper=WARN",
                "logging.level.org.apache.kafka=WARN",
                "logging.level.kafka=WARN",
                "logging.level.state.change.logger=WARN",
                "spring.cloud.stream.kafka.binder.configuration.security.protocol=PLAINTEXT",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.kafka.binder.zkNodes=${spring.embedded.zookeeper.connect}",
                "spring.cloud.stream.binders.kafka-beneficiary-rule.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-onboarding-request.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-onboarding-outcome.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                //endregion

                //region mongodb
                "logging.level.org.mongodb.driver=WARN",
                "logging.level.org.springframework.boot.autoconfigure.mongo.embedded=WARN",
                "spring.mongodb.embedded.version=4.0.21",
                //endregion
        })
@AutoConfigureDataMongo
public abstract class BaseIntegrationTest {
    @Autowired
    protected EmbeddedKafkaBroker kafkaBroker;
    @Autowired
    protected KafkaTemplate<byte[], byte[]> template;

    @Autowired
    protected DroolsRuleRepository droolsRuleRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${spring.cloud.stream.kafka.binder.zkNodes}")
    private String zkNodes;

    @Value("${spring.cloud.stream.bindings.beneficiaryRuleConsumer-in-0.destination}")
    protected String topicBeneficiaryRuleConsumer;
    @Value("${spring.cloud.stream.bindings.admissibilityProcessor-in-0.destination}")
    protected String topicAdmissibilityProcessorRequest;
    @Value("${spring.cloud.stream.bindings.admissibilityProcessor-out-0.destination}")
    protected String topicAdmissibilityProcessorOutcome;

    @BeforeAll
    public static void unregisterPreviouslyKafkaServers() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        ObjectName kafkaServerMbeanName = new ObjectName("kafka.server:type=app-info,id=0");
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        if (mBeanServer.isRegistered(kafkaServerMbeanName)) {
            mBeanServer.unregisterMBean(kafkaServerMbeanName);
        }
    }

    protected Consumer<String, String> getEmbeddedKafkaConsumer(String topic, String groupId) {
        if (!kafkaBroker.getTopics().contains(topic)) {
            kafkaBroker.addTopics(topic);
        }

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(groupId, "true", kafkaBroker);
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, String> consumer = cf.createConsumer();
        kafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic);
        return consumer;
    }

    protected void readFromEmbeddedKafka(String topic, String groupId, java.util.function.Consumer<ConsumerRecord<String, String>> consumeMessage, Integer expectedMessagesCount, Duration timeout) {
        readFromEmbeddedKafka(getEmbeddedKafkaConsumer(topic, groupId), consumeMessage, true, expectedMessagesCount, timeout);
    }

    protected void readFromEmbeddedKafka(Consumer<String, String> consumer, java.util.function.Consumer<ConsumerRecord<String, String>> consumeMessage, boolean consumeFromBeginning, Integer expectedMessagesCount, Duration timeout) {
        if(consumeFromBeginning){
            consumeFromBeginning(consumer);
        }
        int i = 0;
        while (i < expectedMessagesCount) {
            ConsumerRecords<String, String> published = consumer.poll(timeout);
            for (ConsumerRecord<String, String> stringStringConsumerRecord : published) {
                consumeMessage.accept(stringStringConsumerRecord);
                i++;
            }
        }

    }

    protected void consumeFromBeginning(Consumer<String, String> consumer) {
        consumer.seekToBeginning(consumer.assignment());
    }

    protected void publishIntoEmbeddedKafka(String topic, Iterable<Header> headers, String key, Object payload) {
        try {
            publishIntoEmbeddedKafka(topic, headers, key, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void publishIntoEmbeddedKafka(String topic, Iterable<Header> headers, String key, String payload) {
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(topic, null, key==null? null : key.getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8), headers);
        template.send(record);
    }

    protected void wait(int millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
