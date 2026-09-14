package it.gov.pagopa.admissibility.service.onboarding.notifier;

import it.gov.pagopa.admissibility.dto.notification.NotificationQueueDTO;
import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Service
public class OnboardingNotifierServiceImpl implements OnboardingNotifierService {

    private final StreamBridge streamBridge;

    public OnboardingNotifierServiceImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    /** Declared just to let know Spring to connect the producer at startup */
    @Configuration
    static class OnboardingNotifierProducerConfig {
        @Bean
        public Supplier<Flux<Message<EvaluationDTO>>> admissibilityProcessorOut() {
            return Flux::empty;
        }

        @Bean
        public Supplier<Flux<Message<NotificationQueueDTO>>> notificationRequest() {
            return Flux::empty;
        }
    }

    @Override
    public boolean notify(EvaluationDTO evaluationDTO) {
        return streamBridge.send("admissibilityProcessorOut-out-0",
                buildMessage(evaluationDTO));
    }

    @Override
    public boolean notifyNotificationRequest(NotificationQueueDTO notificationQueueDTO) {
        return streamBridge.send("notificationRequest-out-0",
                buildMessage(notificationQueueDTO));
    }

    public static Message<EvaluationDTO> buildMessage(EvaluationDTO evaluationDTO){
        return MessageBuilder.withPayload(evaluationDTO).build();
    }

    public static Message<NotificationQueueDTO> buildMessage(NotificationQueueDTO notificationQueueDTO){
        return MessageBuilder.withPayload(notificationQueueDTO).build();
    }

}
