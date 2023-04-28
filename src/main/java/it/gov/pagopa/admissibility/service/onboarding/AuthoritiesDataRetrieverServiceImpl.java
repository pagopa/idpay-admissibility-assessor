package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.service.onboarding.notifier.OnboardingRescheduleService;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.AnprInvocationService;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.InpsInvocationService;
import it.gov.pagopa.admissibility.service.pdnd.CreateTokenService;
import it.gov.pagopa.admissibility.service.pdnd.UserFiscalCodeService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AuthoritiesDataRetrieverServiceImpl implements AuthoritiesDataRetrieverService {
    private final Long delayMinutes;
    private final boolean nextDay;
    private final OnboardingContextHolderService onboardingContextHolderService;

    private final CreateTokenService createTokenService;
    private final UserFiscalCodeService userFiscalCodeService;
    private final InpsInvocationService inpsInvocationService;
    private final AnprInvocationService anprInvocationService;
    private final OnboardingRescheduleService onboardingRescheduleService;


    public AuthoritiesDataRetrieverServiceImpl(
                                               @Value("${app.onboarding-request.delay-message.delay-minutes}") Long delayMinutes,
                                               @Value("${app.onboarding-request.delay-message.next-day}") boolean nextDay,

                                               OnboardingRescheduleService onboardingRescheduleService,
                                               CreateTokenService createTokenService,
                                               UserFiscalCodeService userFiscalCodeService,
                                               InpsInvocationService inpsInvocationService,
                                               AnprInvocationService anprInvocationService,
                                               OnboardingContextHolderService onboardingContextHolderService) {
        this.onboardingRescheduleService = onboardingRescheduleService;
        this.delayMinutes = delayMinutes;
        this.nextDay = nextDay;
        this.createTokenService = createTokenService;
        this.userFiscalCodeService = userFiscalCodeService;
        this.inpsInvocationService = inpsInvocationService;
        this.anprInvocationService = anprInvocationService;
        this.onboardingContextHolderService = onboardingContextHolderService;
    }

    @Override
    public Mono<OnboardingDTO> retrieve(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, Message<String> message) {
        log.trace("[ONBOARDING_REQUEST] [AUTOMATED_CRITERIA_FIELD_FILL] retrieving automated criteria of user {} into initiative {}", onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());

        List<IseeTypologyEnum> iseeTypes;
        if(onboardingRequest.getIsee()!=null){
            iseeTypes = Collections.emptyList();
        } else {
            iseeTypes = initiativeConfig.getAutomatedCriteria().stream()
                    .filter(c -> OnboardingConstants.CRITERIA_CODE_ISEE.equals(c.getCode()))
                    .map(AutomatedCriteriaDTO::getIseeTypes)
                    .flatMap(Collection::stream)
                    .toList();
        }

        PdndServicesInvocation pdndServicesInvocation = new PdndServicesInvocation(
                onboardingRequest.getIsee() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_ISEE),
                iseeTypes,
                onboardingRequest.getResidence() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_RESIDENCE),
                onboardingRequest.getBirthDate() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_BIRTHDATE)
        );

        if (pdndServicesInvocation.requirePdndInvocation()) {
            ApiKeysPDND pdndApiKeys = onboardingContextHolderService.getPDNDapiKeys(initiativeConfig);
            return  createTokenService.getToken(pdndApiKeys)
                    .zipWith(userFiscalCodeService.getUserFiscalCode(onboardingRequest.getUserId()))
                    .flatMap(t -> invokePdndServices(t.getT1(), onboardingRequest, t.getT2(), pdndServicesInvocation, message, pdndApiKeys.getAgidJwtTokenPayload()));
        }

        return Mono.just(onboardingRequest);
    }

    private Mono<OnboardingDTO> invokePdndServices(String accessToken, OnboardingDTO onboardingRequest, String fiscalCode, PdndServicesInvocation pdndServicesInvocation, Message<String> message, AgidJwtTokenPayload agidJwtTokenPayload) {
        Mono<Optional<ConsultazioneIndicatoreResponseType>> inpsInvocation = pdndServicesInvocation.requireInpsInvocation()
                ? inpsInvocationService.invoke(fiscalCode, pdndServicesInvocation.iseeTypes.get(0)) // TODO invoke all until obtained a result
                : Mono.just(Optional.empty());

        Mono<Optional<RispostaE002OKDTO>> anprInvocation = pdndServicesInvocation.requireAnprInvocation()
                ? anprInvocationService.invoke(accessToken, fiscalCode, agidJwtTokenPayload)
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
                        onboardingRescheduleService.reschedule(onboardingRequest, calcDelay(), "Daily limit reached", message);
                        return null;
                    }
                });
    }

    private void extractPdndResponses(OnboardingDTO onboardingRequest, PdndServicesInvocation pdndServicesInvocation, ConsultazioneIndicatoreResponseType inpsResponse, RispostaE002OKDTO anprResponse) {
        // INPS
        inpsInvocationService.extract(inpsResponse, pdndServicesInvocation.getIsee, onboardingRequest);

        // ANPR
        anprInvocationService.extract(anprResponse, pdndServicesInvocation.getResidence, pdndServicesInvocation.getBirthDate, onboardingRequest);
    }

    private boolean is2retrieve(InitiativeConfig initiativeConfig, String criteriaCode) {
        return (initiativeConfig.getAutomatedCriteriaCodes() != null && initiativeConfig.getAutomatedCriteriaCodes().contains(criteriaCode))
                ||
                (initiativeConfig.getRankingFields() != null && initiativeConfig.getRankingFields().stream().anyMatch(rankingFieldCodes -> criteriaCode.equals(rankingFieldCodes.getFieldCode())));
    }

    private OffsetDateTime calcDelay() {
        LocalDate today = LocalDate.now();
        if (this.nextDay) {
            LocalTime midnight = LocalTime.MIDNIGHT;
            return LocalDateTime.of(today, midnight).plusDays(1).atZone(Utils.ZONE_ID).toOffsetDateTime();
        } else {
            return LocalDateTime.now().plusMinutes(this.delayMinutes).atZone(Utils.ZONE_ID).toOffsetDateTime();
        }
    }

    @AllArgsConstructor
    private static class PdndServicesInvocation {

        boolean getIsee;
        List<IseeTypologyEnum> iseeTypes;
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
