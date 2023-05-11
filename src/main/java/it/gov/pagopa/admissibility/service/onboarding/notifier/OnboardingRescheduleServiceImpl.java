package it.gov.pagopa.admissibility.service.onboarding.notifier;

import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.service.ErrorNotifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
@Service
public class OnboardingRescheduleServiceImpl implements OnboardingRescheduleService {

    private final StreamBridge streamBridge;
    private final ErrorNotifierService errorNotifierService;

    public OnboardingRescheduleServiceImpl(StreamBridge streamBridge, ErrorNotifierService errorNotifierService) {
        this.streamBridge = streamBridge;
        this.errorNotifierService = errorNotifierService;
    }

    @Override
    public void reschedule(OnboardingDTO request, OffsetDateTime rescheduleDateTime, String cause, Message<String> message) {
        log.info("[ONBOARDING_REQUEST] [RESCHEDULE] Rescheduling onboarding request of user {} into initiative {}: {}", request.getUserId(), request.getInitiativeId(), cause);
        Message<OnboardingDTO> delayedMessage = MessageBuilder.withPayload(request)
                .setHeaders(new MessageHeaderAccessor(message))
                .setHeader(ServiceBusMessageHeaders.SCHEDULED_ENQUEUE_TIME, rescheduleDateTime)
                .build();
        if(!streamBridge.send("admissibilityDelayProducer-out-0", delayedMessage)){
            errorNotifierService.notifyAdmissibility(delayedMessage,  "[ONBOARDING_REQUEST] [RETRIEVE_ERROR] Cannot reschedule onboarding request", true, null);
        }
    }
}
