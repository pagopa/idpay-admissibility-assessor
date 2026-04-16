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
    private final IseeThresholdConsultationSoapClient thresholdClient;

    public InpsThresholdRetrieverServiceImpl(
            CriteriaCodeService criteriaCodeService,
            IseeThresholdConsultationSoapClient thresholdClient) {

        this.criteriaCodeService = criteriaCodeService;
        this.thresholdClient = thresholdClient;
    }

    @Override
    public Mono<Optional<List<OnboardingRejectionReason>>> invoke(
            String fiscalCode,
            PdndInitiativeConfig pdndInitiativeConfig,
            PdndServicesInvocation invocation,
            OnboardingDTO onboardingRequest) {

        //  La soglia ISEE è rilevante SOLO se:
        // - la verifica è richiesta
        // - il criterio è ISEE
        // - esiste una thresholdCode
        if (!invocation.requirePdndInvocation()
                || !OnboardingConstants.CRITERIA_CODE_ISEE.equals(invocation.getCode())
                || invocation.getThresholdCode() == null) {

            return MONO_OPTIONAL_EMPTY_LIST;
        }

        return thresholdClient
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
     * Interpreta la risposta INPS sulla soglia ISEE.
     *
     * @return
     *  - emptyList → soglia superata
     *  - lista con KO → soglia non superata
     */
    private List<OnboardingRejectionReason> extractData(
            ConsultazioneSogliaIndicatoreResponseType response) {

        //  chiamata KO
        if (response == null || !EsitoEnum.OK.equals(response.getEsito())) {
            return buildThresholdKoRejection();
        }

        //  dati mancanti
        if (response.getDatiIndicatore() == null
                || response.getDatiIndicatore().getSottoSoglia() == null) {
            return buildThresholdKoRejection();
        }

        boolean sottoSoglia =
                SiNoEnum.SI.equals(response.getDatiIndicatore().getSottoSoglia());

        boolean difformita =
                SiNoEnum.SI.equals(response.getDatiIndicatore().getPresenzaDifformita());

        //  soglia superata (sotto soglia e senza difformità)
        if (sottoSoglia && !difformita) {
            return Collections.emptyList();
        }

        //  soglia non superata
        return buildThresholdKoRejection();
    }

    private List<OnboardingRejectionReason> buildThresholdKoRejection() {

        CriteriaCodeConfig cfg =
                criteriaCodeService.getCriteriaCodeConfig(
                        OnboardingConstants.CRITERIA_CODE_ISEE
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