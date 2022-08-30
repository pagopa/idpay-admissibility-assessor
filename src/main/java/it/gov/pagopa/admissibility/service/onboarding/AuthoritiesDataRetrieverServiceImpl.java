package it.gov.pagopa.admissibility.service.onboarding;

import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Slf4j
public class AuthoritiesDataRetrieverServiceImpl implements AuthoritiesDataRetrieverService{

    private static int counter = 0;
    private final OnboardingContextHolderService onboardingContextHolderService;

    private final StreamBridge streamBridge;

    public AuthoritiesDataRetrieverServiceImpl(OnboardingContextHolderService onboardingContextHolderService, StreamBridge streamBridge) {
        this.onboardingContextHolderService = onboardingContextHolderService;
        this.streamBridge = streamBridge;
    }

    @Override
    public Mono<OnboardingDTO> retrieve(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig) {
        log.trace("[ONBOARDING_REQUEST] [AUTOMATED_CRITERIA_FIELD_FILL] retrieving automated criteria of user {} into initiative {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());

        /* TODO
        * for each initiativeConfig.automatedCriteriaCode,
        *       retrieve the associated authority and field from the Config map (use CriteriaCodeService),
        *       if the OnboardingDTO field's value is null
        *           call the PDND service giving it the token and authority and store the value into the OnboardingDTO relative field
        *           if the call gave threshold error postpone the message and short circuit for the other invocation for the current date
        * if all the calls were successful return a Mono with the request
        */
        if (counter == 1) {
            log.info("[ONBOARDING_REQUEST] [RETRIEVE_ERROR] PDND calls threshold");
            MessageBuilder<OnboardingDTO> delayedMessage = MessageBuilder.withPayload(onboardingRequest)
                    .setHeader(ServiceBusMessageHeaders.SCHEDULED_ENQUEUE_TIME, calcTomorrow());
            streamBridge.send("admissibilityDelayProducer-out-0", delayedMessage.build());
            counter = 0;
            return Mono.empty();
        } else {
            counter++;
            return Mono.just(onboardingRequest);    // TODO
        }

    }

    private LocalDateTime calcTomorrow() {
        LocalDate today = LocalDate.now();
        LocalTime midnight = LocalTime.MIDNIGHT;
        return LocalDateTime.of(today, midnight).plusDays(1);
    }
}
