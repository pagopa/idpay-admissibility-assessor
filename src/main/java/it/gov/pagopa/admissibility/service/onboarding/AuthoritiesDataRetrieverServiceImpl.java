package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprDataRetrieverService;
import it.gov.pagopa.admissibility.connector.soap.inps.service.InpsDataRetrieverService;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.exception.OnboardingException;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.notifier.OnboardingRescheduleService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import it.gov.pagopa.common.utils.CommonConstants;
import lombok.NonNull;
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
import java.util.stream.Stream;

@Service
@Slf4j
public class AuthoritiesDataRetrieverServiceImpl implements AuthoritiesDataRetrieverService {

    private final Long delayMinutes;
    private final boolean nextDay;

    private final UserFiscalCodeService userFiscalCodeService;
    private final InpsDataRetrieverService inpsDataRetrieverService;
    private final AnprDataRetrieverService anprDataRetrieverService;
    private final OnboardingRescheduleService onboardingRescheduleService;
    private final PagoPaAnprPdndConfig pagoPaAnprPdndConfig;

    public AuthoritiesDataRetrieverServiceImpl(
            @Value("${app.onboarding-request.delay-message.delay-minutes}") Long delayMinutes,
            @Value("${app.onboarding-request.delay-message.next-day}") boolean nextDay,

            OnboardingRescheduleService onboardingRescheduleService,
            UserFiscalCodeService userFiscalCodeService,
            InpsDataRetrieverService inpsDataRetrieverService,
            AnprDataRetrieverService anprDataRetrieverService,
            PagoPaAnprPdndConfig pagoPaAnprPdndConfig) {
        this.onboardingRescheduleService = onboardingRescheduleService;
        this.delayMinutes = delayMinutes;
        this.nextDay = nextDay;
        this.userFiscalCodeService = userFiscalCodeService;
        this.inpsDataRetrieverService = inpsDataRetrieverService;
        this.anprDataRetrieverService = anprDataRetrieverService;
        this.pagoPaAnprPdndConfig = pagoPaAnprPdndConfig;
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

        PdndServicesInvocation pdndServicesInvocation = configurePdndServicesInvocation(onboardingRequest, initiativeConfig, iseeTypes);

        if (pdndServicesInvocation.requirePdndInvocation()) {
            //PdndInitiativeConfig pdndInitiativeConfig = initiativeConfig.getPdndInitiativeConfig(); TODO it should be read from initiative
            return  userFiscalCodeService.getUserFiscalCode(onboardingRequest.getUserId())
                    .flatMap(fiscalCode -> invokePdndServices(onboardingRequest, fiscalCode, pdndServicesInvocation, message, pagoPaAnprPdndConfig));
        }

        return Mono.just(onboardingRequest);
    }

    @NonNull
    private PdndServicesInvocation configurePdndServicesInvocation(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, List<IseeTypologyEnum> iseeTypes) {
        return new PdndServicesInvocation(
                onboardingRequest.getIsee() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_ISEE),
                iseeTypes,
                onboardingRequest.getResidence() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_RESIDENCE),
                onboardingRequest.getBirthDate() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_BIRTHDATE)
        );
    }

    private Mono<OnboardingDTO> invokePdndServices(OnboardingDTO onboardingRequest, String fiscalCode, PdndServicesInvocation pdndServicesInvocation, Message<String> message, PdndInitiativeConfig pdndInitiativeConfig) {
        Mono<Optional<List<OnboardingRejectionReason>>> inpsInvocation =
                inpsDataRetrieverService.invoke(fiscalCode, pdndInitiativeConfig, pdndServicesInvocation, onboardingRequest);

        Mono<Optional<List<OnboardingRejectionReason>>> anprInvocation =
                anprDataRetrieverService.invoke(fiscalCode, pdndInitiativeConfig, pdndServicesInvocation, onboardingRequest);

        return inpsInvocation
                .zipWith(anprInvocation)
                // Handle reschedule of failed invocation, storing the successful if any
                .mapNotNull(t -> {

                    List<OnboardingRejectionReason> rejectionReasons = Stream.concat(
                            t.getT1().stream().flatMap(Collection::stream),
                            t.getT2().stream().flatMap(Collection::stream)
                    ).toList();

                    if(!rejectionReasons.isEmpty()){
                        throw new OnboardingException(rejectionReasons, "Cannot retrieve all required authorities data");
                    }

                    // not require INPS or it returned data
                    if (t.getT1().isPresent()
                            &&
                            // not require ANPR or it returned data
                            t.getT2().isPresent()) {
                        return onboardingRequest;
                    } else {
                        onboardingRescheduleService.reschedule(onboardingRequest, calcDelay(), "Daily limit reached", message);
                        return null;
                    }
                });
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
            return LocalDateTime.of(today, midnight).plusDays(1).atZone(CommonConstants.ZONEID).toOffsetDateTime();
        } else {
            return LocalDateTime.now().plusMinutes(this.delayMinutes).atZone(CommonConstants.ZONEID).toOffsetDateTime();
        }
    }

}
