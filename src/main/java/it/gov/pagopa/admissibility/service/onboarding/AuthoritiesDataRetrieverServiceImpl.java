package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprDataRetrieverService;
import it.gov.pagopa.admissibility.connector.soap.inps.service.InpsDataRetrieverService;
import it.gov.pagopa.admissibility.connector.soap.inps.service.InpsThresholdRetrieverService;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.exception.OnboardingException;
import it.gov.pagopa.admissibility.exception.PdndException;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
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
    private final InpsThresholdRetrieverService inpsThresholdRetrieverService;
    private final AnprDataRetrieverService anprDataRetrieverService;
    private final OnboardingRescheduleService onboardingRescheduleService;
    private final PagoPaAnprPdndConfig pagoPaAnprPdndConfig;

    public AuthoritiesDataRetrieverServiceImpl(
            @Value("${app.onboarding-request.delay-message.delay-minutes}") Long delayMinutes,
            @Value("${app.onboarding-request.delay-message.next-day}") boolean nextDay,

            OnboardingRescheduleService onboardingRescheduleService,
            UserFiscalCodeService userFiscalCodeService,
            InpsDataRetrieverService inpsDataRetrieverService,
            InpsThresholdRetrieverService inpsThresholdRetrieverService,
            AnprDataRetrieverService anprDataRetrieverService,
            PagoPaAnprPdndConfig pagoPaAnprPdndConfig) {
        this.onboardingRescheduleService = onboardingRescheduleService;
        this.delayMinutes = delayMinutes;
        this.nextDay = nextDay;
        this.userFiscalCodeService = userFiscalCodeService;
        this.inpsDataRetrieverService = inpsDataRetrieverService;
        this.inpsThresholdRetrieverService = inpsThresholdRetrieverService;
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
        log.debug("[ONBOARDING_REQUEST] Requesting pdnd data {} for userId {} and initiativeId {}", pdndServicesInvocation, onboardingRequest.getUserId(), onboardingRequest.getInitiativeId());


        if (pdndServicesInvocation.requirePdndInvocation()) {
            return  userFiscalCodeService.getUserFiscalCode(onboardingRequest.getUserId())
                    .flatMap(fiscalCode -> invokePdndServices(onboardingRequest, fiscalCode, pdndServicesInvocation, message));
        }

        return Mono.just(onboardingRequest);
    }

    @NonNull
    private PdndServicesInvocation configurePdndServicesInvocation(OnboardingDTO onboardingRequest, InitiativeConfig initiativeConfig, List<IseeTypologyEnum> iseeTypes) {
        return new PdndServicesInvocation(
                onboardingRequest.getIsee() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_ISEE),
                iseeTypes,
                onboardingRequest.getResidence() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_RESIDENCE),
                onboardingRequest.getBirthDate() == null && is2retrieve(initiativeConfig, OnboardingConstants.CRITERIA_CODE_BIRTHDATE),
                onboardingRequest.getVerifyIsee() && initiativeConfig.getIseeThresholdCode() != null,
                initiativeConfig.getIseeThresholdCode()
        );
    }

    private Mono<OnboardingDTO> invokePdndServices(OnboardingDTO onboardingRequest, String fiscalCode, PdndServicesInvocation pdndServicesInvocation, Message<String> message) {


        Mono<Optional<List<OnboardingRejectionReason>>> inpsInvocation =
                pdndServicesInvocation.getIseeThresholdCode() == null ?
                inpsDataRetrieverService.invoke(fiscalCode, pagoPaAnprPdndConfig.getPagopaPdndConfiguration().get("c001") , pdndServicesInvocation, onboardingRequest)
                : inpsThresholdRetrieverService.invoke(fiscalCode, pagoPaAnprPdndConfig.getPagopaPdndConfiguration().get("c001") , pdndServicesInvocation, onboardingRequest);

        Mono<Optional<List<OnboardingRejectionReason>>> anprInvocationSingle =
                anprDataRetrieverService.invoke(fiscalCode, pagoPaAnprPdndConfig.getPagopaPdndConfiguration().get("c001")  , pdndServicesInvocation, onboardingRequest);


        return Mono.zip(
                        inpsInvocation,
                        anprInvocationSingle
                )
                .mapNotNull(t -> {

                    Optional<List<OnboardingRejectionReason>> inpsResult = t.getT1();
                    Optional<List<OnboardingRejectionReason>> anprSingleResult = t.getT2();

                    List<OnboardingRejectionReason> rejectionReasons = Stream.of(inpsResult, anprSingleResult)
                            .filter(Optional::isPresent)
                            .flatMap(optional -> optional.get().stream())
                            .toList();

                    if(!rejectionReasons.isEmpty()){
                        log.debug("[ONBOARDING_REQUEST][ONBOARDING_KO] Authorities data retrieve returned rejection reasons: {}", rejectionReasons);
                        throw new OnboardingException(rejectionReasons, "Cannot retrieve all required authorities data");
                    }


                    if (t.getT1().isPresent()
                            &&
                            t.getT2().isPresent()) {
                        return onboardingRequest;
                    } else {
//                        onboardingRescheduleService.reschedule(onboardingRequest, calcDelay(), "Daily limit reached", message);
//                        return null;
                        throw new PdndException();

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
