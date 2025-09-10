package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.soap.inps.exception.InpsDailyRequestLimitException;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.generated.soap.ws.client.soglia.ConsultazioneSogliaIndicatoreResponseType;
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

    private final List<OnboardingRejectionReason> iseeNotFoundRejectionReason;
    private final Mono<List<OnboardingRejectionReason>> iseeNotFoundRejectionReasonMono;


    public InpsThresholdRetrieverServiceImpl(CriteriaCodeService criteriaCodeService, IseeThresholdConsultationSoapClient iseeConsultationSoapClient) {
        this.criteriaCodeService = criteriaCodeService;
        this.iseeThresholdConsultationSoapClient = iseeConsultationSoapClient;

        this.iseeNotFoundRejectionReason = buildMissingIseeRejectionReasons();
        this.iseeNotFoundRejectionReasonMono = Mono.just(iseeNotFoundRejectionReason);
    }

    private boolean accept(PdndServicesInvocation pdndServicesInvocation) {
        return pdndServicesInvocation.isVerifyIseeThreshold();
    }

    @Override
    public Mono<Optional<List<OnboardingRejectionReason>>> invoke(
            String fiscalCode,
            PdndInitiativeConfig pdndInitiativeConfig,
            PdndServicesInvocation pdndServicesInvocation,
            OnboardingDTO onboardingRequest) {
        if (!accept(pdndServicesInvocation)) {
            return MONO_OPTIONAL_EMPTY_LIST;
        }
        return processResponse(fiscalCode, pdndServicesInvocation, onboardingRequest)
                .map(Optional::of)
                .switchIfEmpty(MONO_EMPTY_RESPONSE)

                .onErrorResume(InpsDailyRequestLimitException.class, e -> {
                    log.debug("[ONBOARDING_REQUEST][INPS_INVOCATION] Daily limit occurred when calling ANPR service", e);
                    return MONO_EMPTY_RESPONSE;
                });
    }

    private Mono<List<OnboardingRejectionReason>> processResponse(String fiscalCode, PdndServicesInvocation pdndServicesInvocation, OnboardingDTO onboardingRequest) {
        Mono<List<OnboardingRejectionReason>> inpsInvoke = iseeNotFoundRejectionReasonMono;

        return iseeThresholdConsultationSoapClient
                .verifyThresholdIsee(fiscalCode, pdndServicesInvocation.getIseeThresholdCode())
                .map(inpsResponse -> extractData(inpsResponse, onboardingRequest))
                .switchIfEmpty(inpsInvoke);
    }

    private List<OnboardingRejectionReason> extractData(ConsultazioneSogliaIndicatoreResponseType inpsResponse, OnboardingDTO onboardingRequest) {
        if (inpsResponse != null) {
            onboardingRequest.setUnderThreshold(getIseeFromResponse(inpsResponse));
        }

        if(onboardingRequest.getUnderThreshold() == null) {
            log.debug("[ONBOARDING_REQUEST][INPS_INVOCATION] ISEE threshold info not compatible");
            return iseeNotFoundRejectionReason;
        }
        return Collections.emptyList();
    }

    private List<OnboardingRejectionReason> buildMissingIseeRejectionReasons() {
        CriteriaCodeConfig criteriaCodeConfig = criteriaCodeService.getCriteriaCodeConfig(OnboardingConstants.CRITERIA_CODE_ISEE);
        return List.of(new OnboardingRejectionReason(
                        OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                        OnboardingConstants.REJECTION_REASON_ISEE_TYPE_KO,
                        criteriaCodeConfig.getAuthority(),
                        criteriaCodeConfig.getAuthorityLabel(),
                        "Soglia ISEE non disponibile"
                )
        );
    }

    private Boolean getIseeFromResponse(ConsultazioneSogliaIndicatoreResponseType inpsResponse) {
        if(inpsResponse.getDatiIndicatore() != null && inpsResponse.getDatiIndicatore().getSottoSoglia()!=null) {
            return SiNoEnum.SI.equals(inpsResponse.getDatiIndicatore().getSottoSoglia()) ? Boolean.TRUE : Boolean.FALSE;
        }
        return null;

    }
}
