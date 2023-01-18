package it.gov.pagopa.admissibility.service.onboarding;

import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.AnprInvocationService;
import it.gov.pagopa.admissibility.service.pdnd.CreateTokenService;
import it.gov.pagopa.admissibility.service.pdnd.UserFiscalCodeService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
public class AuthoritiesDataRetrieverServiceImpl implements AuthoritiesDataRetrieverService {
    private final Long delaySeconds;
    private final boolean nextDay;

    private final CreateTokenService createTokenService;
    private final UserFiscalCodeService userFiscalCodeService;
    private final AnprInvocationService anprInvocationService;
    private final StreamBridge streamBridge;
    private final OnboardingContextHolderService onboardingContextHolderService;


    public AuthoritiesDataRetrieverServiceImpl(StreamBridge streamBridge,
                                               @Value("${app.onboarding-request.delay-message.delay-duration}") Long delaySeconds,
                                               @Value("${app.onboarding-request.delay-message.next-day}") boolean nextDay,
                                               CreateTokenService createTokenService,
                                               UserFiscalCodeService userFiscalCodeService,
                                               AnprInvocationService anprInvocationService,
                                               OnboardingContextHolderService onboardingContextHolderService) {
        this.streamBridge = streamBridge;
        this.delaySeconds = delaySeconds;
        this.nextDay = nextDay;
        this.createTokenService = createTokenService;
        this.userFiscalCodeService = userFiscalCodeService;
        this.anprInvocationService = anprInvocationService;
        this.onboardingContextHolderService = onboardingContextHolderService;
    }

    @Override
    public Mono<OnboardingDTO> retrieve(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message) {
        log.trace("[ONBOARDING_REQUEST] [AUTOMATED_CRITERIA_FIELD_FILL] retrieving automated criteria of user {} into initiative {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());

        PdndServicesInvocation pdndServicesInvocation = new PdndServicesInvocation(
                onboardingRequest.getIsee() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_ISEE),
                onboardingRequest.getResidence() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_RESIDENCE),
                onboardingRequest.getBirthDate() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_BIRTHDATE)
        );

        if (pdndServicesInvocation.requirePdndInvocation()) {
            return  createTokenService.getToken(onboardingContextHolderService.getPDNDapiKeys(initiativeConfig))
                    .zipWith(userFiscalCodeService.getUserFiscalCode(onboardingRequest.getUserId()))
                    .flatMap(t -> invokePdndServices(t.getT1(), onboardingRequest, t.getT2(), pdndServicesInvocation, message));
        }

        return Mono.just(onboardingRequest);
    }

    private Mono<OnboardingDTO> invokePdndServices(String accessToken, OnboardingDTO onboardingRequest, String fiscalCode, PdndServicesInvocation pdndServicesInvocation, Message<String> message) {
        Mono<Optional<Object>> inpsInvocation = pdndServicesInvocation.requireInpsInvocation()
                ? Mono.just(Optional.of("TODO Call INPS Service"))
                : Mono.just(Optional.empty());

        Mono<Optional<RispostaE002OKDTO>> anprInvocation = pdndServicesInvocation.requireAnprInvocation()
                ? anprInvocationService.invoke(accessToken, fiscalCode)
                : Mono.just(Optional.empty());

        return inpsInvocation
                .zipWith(anprInvocation)
                // Handle reschedule of failed invocation, storing the successful if any
                .mapNotNull(t -> {
                    extractPdndResponses(
                            onboardingRequest,
                            pdndServicesInvocation,
                            t.getT1().orElse(null),
                            t.getT2().orElse(null));

                    if ((!pdndServicesInvocation.requireInpsInvocation() || t.getT1().isPresent())
                            &&
                            (!pdndServicesInvocation.requireAnprInvocation() || t.getT2().isPresent())) {
                        return onboardingRequest;
                    } else {
                        rischeduleOnboardingRequest(onboardingRequest, message);
                        return null;
                    }
                });
    }

    private void extractPdndResponses(OnboardingDTO onboardingRequest, PdndServicesInvocation pdndServicesInvocation, Object inpsResponse, RispostaE002OKDTO anprResponse) {
        if (inpsResponse != null && pdndServicesInvocation.getIsee) {
            onboardingRequest.setIsee(new BigDecimal(userIdBasedIntegerGenerator(onboardingRequest).nextInt(1_000, 100_000)));  // TODO
        }

        anprInvocationService.extract(anprResponse, pdndServicesInvocation.getResidence, pdndServicesInvocation.getBirthDate, onboardingRequest);
    }

    private boolean is2retrieve(InitiativeConfig initiativeConfig, String criteriaCode) {
        return (initiativeConfig.getAutomatedCriteriaCodes() != null && initiativeConfig.getAutomatedCriteriaCodes().contains(criteriaCode))
                ||
                (initiativeConfig.getRankingFields() != null && initiativeConfig.getRankingFields().stream().anyMatch(rankingFieldCodes -> criteriaCode.equals(rankingFieldCodes.getFieldCode())));
    }

    private void rischeduleOnboardingRequest(OnboardingDTO onboardingRequest, Message<String> message) {
        log.info("[ONBOARDING_REQUEST] [RETRIEVE_ERROR] PDND calls threshold reached");
        MessageBuilder<OnboardingDTO> delayedMessage = MessageBuilder.withPayload(onboardingRequest)
                .setHeaders(new MessageHeaderAccessor(message))
                .setHeader(ServiceBusMessageHeaders.SCHEDULED_ENQUEUE_TIME, calcDelay());
        streamBridge.send("admissibilityDelayProducer-out-0", delayedMessage.build());
    }

    private OffsetDateTime calcDelay() {
        LocalDate today = LocalDate.now();
        if (this.nextDay) {
            LocalTime midnight = LocalTime.MIDNIGHT;
            return LocalDateTime.of(today, midnight).plusDays(1).atZone(ZoneId.of("Europe/Rome")).toOffsetDateTime();
        } else {
            return LocalDateTime.now().plusSeconds(this.delaySeconds).atZone(ZoneId.of("Europe/Rome")).toOffsetDateTime();
        }
    }

    private static Random userIdBasedIntegerGenerator(OnboardingDTO onboardingRequest) {
        @SuppressWarnings("squid:S2245")
        Random random = new Random(onboardingRequest.getUserId().hashCode());
        return random;
    }

    @AllArgsConstructor
    private static class PdndServicesInvocation {

        boolean getIsee;
        boolean getResidence;
        boolean getBirthDate;

        boolean requirePdndInvocation() {
            return getIsee || getResidence || getBirthDate;
        }

        boolean requireInpsInvocation() {
            return getIsee;
        }

        boolean requireAnprInvocation() {
            return getResidence || getBirthDate;
        }
    }
}


