package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.soap.inps.exception.InpsDailyRequestLimitException;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.ConsultazioneSogliaIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.EsitoEnum;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.SiNoEnum;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class InpsThresholdRetrieverServiceImpl implements InpsThresholdRetrieverService {

    private final CriteriaCodeService criteriaCodeService;
    private final IseeThresholdConsultationSoapClient iseeThresholdConsultationSoapClient;

    public InpsThresholdRetrieverServiceImpl(
            CriteriaCodeService criteriaCodeService,
            IseeThresholdConsultationSoapClient iseeThresholdConsultationSoapClient) {

        this.criteriaCodeService = criteriaCodeService;
        this.iseeThresholdConsultationSoapClient = iseeThresholdConsultationSoapClient;
    }

    @Override
    public Mono<Optional<List<OnboardingRejectionReason>>> invoke(
            String fiscalCode,
            PdndInitiativeConfig pdndInitiativeConfig,
            PdndServicesInvocation invocation,
            OnboardingDTO onboardingRequest) {

        //  soglia INPS solo se:
        // - verify == true
        // - code == ISEE
        // - thresholdCode != null
        if (!invocation.requirePdndInvocation()
                || !OnboardingConstants.CRITERIA_CODE_ISEE.equalsIgnoreCase(invocation.getCode())
                || invocation.getThresholdCode() == null) {

            return MONO_OPTIONAL_EMPTY_LIST;
        }

        return iseeThresholdConsultationSoapClient
                .verifyThresholdIsee(fiscalCode, invocation.getThresholdCode())
                .map(response ->
                        Optional.of(
                                extractData(response)
                        )
                )
                .switchIfEmpty(MONO_EMPTY_RESPONSE)
                .onErrorResume(InpsDailyRequestLimitException.class, e -> {
                    log.debug(
                            "[ONBOARDING_REQUEST][INPS_THRESHOLD] Daily limit occurred when calling INPS threshold service",
                            e
                    );
                    return MONO_EMPTY_RESPONSE;
                });
    }

    /**
     * - emptyList -> verifica OK
     * - lista con KO -> verifica KO
     */
    private List<OnboardingRejectionReason> extractData(
            ConsultazioneSogliaIndicatoreResponseType response) {

        // chiamata KO o risposta non valida
        if (response == null || !EsitoEnum.OK.equals(response.getEsito())) {
            return buildThresholdKoRejection();
        }

        if (response.getDatiIndicatore() == null
                || response.getDatiIndicatore().getSottoSoglia() == null) {
            return buildThresholdKoRejection();
        }

        boolean sottoSoglia =
                SiNoEnum.SI.equals(response.getDatiIndicatore().getSottoSoglia());

        boolean difformita =
                SiNoEnum.SI.equals(response.getDatiIndicatore().getPresenzaDifformita());

        // se sotto soglia e NON difforme
        if (sottoSoglia && !difformita) {
            return Collections.emptyList();
        }

        // soglia non rispettata
        return buildThresholdKoRejection();
    }

    private List<OnboardingRejectionReason> buildThresholdKoRejection() {

        CriteriaCodeConfig cfg =
                criteriaCodeService.getCriteriaCodeConfig(
                        OnboardingConstants.CRITERIA_CODE_ISEE.toLowerCase()
                );

        return List.of(
                new OnboardingRejectionReason(
                        OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                        OnboardingConstants.REJECTION_REASON_ISEE_TYPE_KO,
                        cfg.getAuthority(),
                        cfg.getAuthorityLabel(),
                        "Soglia ISEE non superata"
                )
        );
    }
}