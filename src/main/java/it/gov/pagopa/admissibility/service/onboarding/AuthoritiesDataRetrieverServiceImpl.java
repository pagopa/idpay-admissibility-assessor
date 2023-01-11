package it.gov.pagopa.admissibility.service.onboarding;

import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoGeneralitaDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoResidenzaDTO;
import it.gov.pagopa.admissibility.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.rest.anpr.exception.AnprDailyRequestLimitException;
import it.gov.pagopa.admissibility.service.pdnd.CreateTokenService;
import it.gov.pagopa.admissibility.service.pdnd.UserFiscalCodeService;
import it.gov.pagopa.admissibility.service.pdnd.residence.ResidenceAssessmentService;
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
import reactor.util.function.Tuple2;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;

@Service
@Slf4j
public class AuthoritiesDataRetrieverServiceImpl implements AuthoritiesDataRetrieverService{
    private final Long delaySeconds;
    private final boolean nextDay;

    private final CreateTokenService createTokenService;
    private final UserFiscalCodeService userFiscalCodeService;
    private final ResidenceAssessmentService residenceAssessmentService;
    private final TipoResidenzaDTO2ResidenceMapper residenceMapper;

    private final StreamBridge streamBridge;


    public AuthoritiesDataRetrieverServiceImpl(StreamBridge streamBridge,
                                               @Value("${app.onboarding-request.delay-message.delay-duration}") Long delaySeconds,
                                               @Value("${app.onboarding-request.delay-message.next-day}") boolean nextDay,
                                               CreateTokenService createTokenService,
                                               UserFiscalCodeService userFiscalCodeService,
                                               ResidenceAssessmentService residenceAssessmentService,
                                               TipoResidenzaDTO2ResidenceMapper residenceMapper) {
        this.streamBridge = streamBridge;
        this.delaySeconds = delaySeconds;
        this.nextDay = nextDay;
        this.createTokenService = createTokenService;
        this.userFiscalCodeService = userFiscalCodeService;
        this.residenceAssessmentService = residenceAssessmentService;
        this.residenceMapper = residenceMapper;
    }

    @Override
    public Mono<OnboardingDTO> retrieve(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message) {
        log.trace("[ONBOARDING_REQUEST] [AUTOMATED_CRITERIA_FIELD_FILL] retrieving automated criteria of user {} into initiative {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());

        /* TODO
        * for each initiativeConfig.automatedCriteriaCode,
        *       retrieve the associated authority and field from the Config map (use CriteriaCodeService),
        *       if the OnboardingDTO field's value is null
        *           call the PDND service giving it the token and authority and store the value into the OnboardingDTO relative field
        *           if the call gave threshold error postpone the message and short circuit for the other invocation for the current date
        * if all the calls were successful return a Mono with the request
        */
        PdndServicesInvocation pdndServicesInvocation = new PdndServicesInvocation(
        onboardingRequest.getIsee() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_ISEE),
        onboardingRequest.getResidence() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_RESIDENCE),
        onboardingRequest.getBirthDate() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_BIRTHDATE)
        );

        if (pdndServicesInvocation.requirePdndInvocation()) {
            return  createTokenService.getToken(initiativeConfig.getPdndToken())
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
                ? residenceAssessmentService.getResidenceAssessment(accessToken, fiscalCode).map(Optional::of)
                .onErrorResume(AnprDailyRequestLimitException.class, e -> {
                    log.debug("[ONBOARDING_REQUEST][RESIDENCE_ASSESSMENT] ", e);
                    return Mono.just(Optional.empty());
                })
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
            onboardingRequest.setIsee(BigDecimal.TEN);  // TODO
        }
        if (anprResponse != null) {
            TipoResidenzaDTO residenza = anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getResidenza().get(0);
            TipoGeneralitaDTO generalita = anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita();

            if (pdndServicesInvocation.getResidence) {
                onboardingRequest.setResidence(residenceMapper.apply(residenza));
            }
            if (pdndServicesInvocation.getBirthDate) {
                onboardingRequest.setBirthDate(getBirthDateFromAnpr(generalita));
            }
        }
    }

    private boolean is2retrieve(InitiativeConfig initiativeConfig, String criteriaCode) {
        return (initiativeConfig.getAutomatedCriteriaCodes()!=null && initiativeConfig.getAutomatedCriteriaCodes().contains(criteriaCode))
                ||
                (initiativeConfig.getRankingFields()!=null && initiativeConfig.getRankingFields().stream().anyMatch(rankingFieldCodes -> criteriaCode.equals(rankingFieldCodes.getFieldCode())));
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
        if(this.nextDay) {
            LocalTime midnight = LocalTime.MIDNIGHT;
            return LocalDateTime.of(today, midnight).plusDays(1).atZone(ZoneId.of("Europe/Rome")).toOffsetDateTime();
        } else {
            return LocalDateTime.now().plusSeconds(this.delaySeconds).atZone(ZoneId.of("Europe/Rome")).toOffsetDateTime();
        }
    }

    private BirthDate getBirthDateFromAnpr(TipoGeneralitaDTO generalita) {
        return BirthDate.builder()
                .age(Period.between(
                        LocalDate.parse(generalita.getDataNascita()),
                        LocalDate.now())
                        .getYears())
                .year(generalita.getSenzaGiornoMese())
                .build();
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


