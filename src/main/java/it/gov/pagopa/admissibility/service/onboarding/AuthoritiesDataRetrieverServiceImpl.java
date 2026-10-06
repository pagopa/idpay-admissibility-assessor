package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.config.CriteriaCodeConfigs;
import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprDataRetrieverService;
import it.gov.pagopa.admissibility.connector.soap.inps.service.InpsDataRetrieverService;
import it.gov.pagopa.admissibility.connector.soap.inps.service.InpsThresholdRetrieverService;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.VerifyDTO;
import it.gov.pagopa.admissibility.exception.PdndException;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.notifier.OnboardingRescheduleService;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import it.gov.pagopa.common.utils.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
    private final CriteriaCodeConfigs criteriaCodeConfigs;

    public AuthoritiesDataRetrieverServiceImpl(
            @Value("${app.onboarding-request.delay-message.delay-minutes}") Long delayMinutes,
            @Value("${app.onboarding-request.delay-message.next-day}") boolean nextDay,
            OnboardingRescheduleService onboardingRescheduleService,
            UserFiscalCodeService userFiscalCodeService,
            InpsDataRetrieverService inpsDataRetrieverService,
            InpsThresholdRetrieverService inpsThresholdRetrieverService,
            AnprDataRetrieverService anprDataRetrieverService,
            PagoPaAnprPdndConfig pagoPaAnprPdndConfig,
            CriteriaCodeConfigs criteriaCodeConfigs) {

        this.delayMinutes = delayMinutes;
        this.nextDay = nextDay;
        this.onboardingRescheduleService = onboardingRescheduleService;
        this.userFiscalCodeService = userFiscalCodeService;
        this.inpsDataRetrieverService = inpsDataRetrieverService;
        this.inpsThresholdRetrieverService = inpsThresholdRetrieverService;
        this.anprDataRetrieverService = anprDataRetrieverService;
        this.pagoPaAnprPdndConfig = pagoPaAnprPdndConfig;
        this.criteriaCodeConfigs = criteriaCodeConfigs;
    }

    public Mono<OnboardingDTO> retrieve(
            OnboardingDTO onboardingRequest,
            InitiativeConfig initiativeConfig,
            Message<String> message) {

        if (onboardingRequest.getVerifies().stream()
                .allMatch(v -> v.getReasonList() != null)) {
            return Mono.just(onboardingRequest);
        }

        return userFiscalCodeService.getUserFiscalCode(onboardingRequest.getUserId())
                .flatMap(fiscalCode ->
                        Flux.fromIterable(onboardingRequest.getVerifies())
                                .concatMap(verify -> {

                                    if (!verify.isVerify()) {
                                        verify.setReasonList(Collections.emptyList());
                                        return Mono.empty();
                                    }

                                    PdndServicesInvocation invocation =
                                            buildInvocation(verify);

                                    return invokePdndServices(
                                            fiscalCode,
                                            invocation,
                                            onboardingRequest,
                                            message)
                                            .doOnNext(verify::setReasonList)
                                            .then();
                                })
                                .then(Mono.just(onboardingRequest))
                );
    }


    private PdndServicesInvocation buildInvocation(VerifyDTO verify) {
        // DTO minimal: code + verify + thresholdCode
        return new PdndServicesInvocation(
                verify.getCode(),
                verify.isVerify(),
                verify.getThresholdCode()
        );
    }

    private Mono<List<OnboardingRejectionReason>> invokePdndServices(
            String fiscalCode,
            PdndServicesInvocation invocation,
            OnboardingDTO onboardingRequest,
            Message<String> message) {

        // Recupero criterio
        CriteriaCodeConfigs.CriteriaConfig cfg =
                criteriaCodeConfigs.getConfigs().get(invocation.getCode());

        if (cfg == null) {
            throw new IllegalArgumentException(
                    "Missing criteria-code-config for code: " + invocation.getCode()
            );
        }

        // Recupero PDND client (c001 / c021)
        String pdndClient = cfg.getPdndClient();

        PdndInitiativeConfig pdndConfig =
                pagoPaAnprPdndConfig
                        .getPagopaPdndConfiguration()
                        .get(pdndClient);

        if (pdndConfig == null) {
            throw new IllegalStateException(
                    "Missing PDND configuration for client: " + pdndClient
            );
        }

        // Scelta servizio su authority
        Mono<Optional<List<OnboardingRejectionReason>>> call =
                switch (cfg.getAuthority()) {

                    case "INPS" -> invocation.getThresholdCode() == null
                            ? inpsDataRetrieverService.invoke(
                            fiscalCode, pdndConfig, invocation, onboardingRequest)
                            : inpsThresholdRetrieverService.invoke(
                            fiscalCode, pdndConfig, invocation, onboardingRequest);

                    case "AGID", "ANPR" ->
                            anprDataRetrieverService.invoke(
                                    fiscalCode, pdndConfig, invocation, onboardingRequest);

                    default -> throw new IllegalArgumentException(
                            "Unsupported authority: " + cfg.getAuthority());
                };

        // Interpretazione risultato
        return call.map(optional -> {

            if (optional.isEmpty()) {
                // PDND temporaneamente non disponibile → RESCHEDULE
                OffsetDateTime nextAttempt = calcDelay();

                onboardingRescheduleService.reschedule(
                        onboardingRequest,
                        nextAttempt,
                        "PDND temporary failure",
                        message
                );

                throw new PdndException();
            }

            // lista vuota = verify OK
            // lista valorizzata = verify KO
            return optional.get();
        });
    }

    private OffsetDateTime calcDelay() {
        LocalDate today = LocalDate.now();
        if (this.nextDay) {
            return LocalDateTime.of(today, LocalTime.MIDNIGHT)
                    .plusDays(1)
                    .atZone(CommonConstants.ZONEID)
                    .toOffsetDateTime();
        }
        return LocalDateTime.now()
                .plusMinutes(this.delayMinutes)
                .atZone(CommonConstants.ZONEID)
                .toOffsetDateTime();
    }


}