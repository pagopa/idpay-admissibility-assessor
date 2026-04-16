package it.gov.pagopa.admissibility.service.onboarding;

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
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AuthoritiesDataRetrieverServiceImpl
        implements AuthoritiesDataRetrieverService {

    private final UserFiscalCodeService userFiscalCodeService;
    private final InpsDataRetrieverService inpsDataRetrieverService;
    private final InpsThresholdRetrieverService inpsThresholdRetrieverService;
    private final AnprDataRetrieverService anprDataRetrieverService;
    private final PagoPaAnprPdndConfig pagoPaAnprPdndConfig;

    public AuthoritiesDataRetrieverServiceImpl(
            UserFiscalCodeService userFiscalCodeService,
            InpsDataRetrieverService inpsDataRetrieverService,
            InpsThresholdRetrieverService inpsThresholdRetrieverService,
            AnprDataRetrieverService anprDataRetrieverService,
            PagoPaAnprPdndConfig pagoPaAnprPdndConfig) {

        this.userFiscalCodeService = userFiscalCodeService;
        this.inpsDataRetrieverService = inpsDataRetrieverService;
        this.inpsThresholdRetrieverService = inpsThresholdRetrieverService;
        this.anprDataRetrieverService = anprDataRetrieverService;
        this.pagoPaAnprPdndConfig = pagoPaAnprPdndConfig;
    }

    @Override
    public Mono<OnboardingDTO> retrieve(OnboardingDTO onboardingRequest,
                                        InitiativeConfig initiativeConfig,
                                        Message<String> message) {

        log.trace(
                "[ONBOARDING_REQUEST] retrieving authorities data for user {} initiative {}",
                onboardingRequest.getUserId(),
                onboardingRequest.getInitiativeId()
        );

        return userFiscalCodeService
                .getUserFiscalCode(onboardingRequest.getUserId())
                .flatMap(fiscalCode ->
                        processVerifies(onboardingRequest, fiscalCode)
                );
    }

    /**
     * Processa TUTTE le VerifyDTO dell’onboarding
     */
    private Mono<OnboardingDTO> processVerifies(OnboardingDTO onboardingRequest,
                                                String fiscalCode) {

        return Mono.fromRunnable(() -> {

            for (VerifyDTO verify : onboardingRequest.getVerifies()) {

                // ✅ Verifica non richiesta → OK implicito
                if (!verify.isVerify()) {
                    onboardingRequest.addResultVerify(verify.getCode(), true);
                    continue;
                }

                Authority authority = resolveAuthority(verify.getCode());

                Boolean result = invokeAuthority(
                        authority,
                        verify,
                        onboardingRequest,
                        fiscalCode
                ).block();

                onboardingRequest.addResultVerify(
                        verify.getCode(),
                        Boolean.TRUE.equals(result)
                );
            }
        }).thenReturn(onboardingRequest);
    }

    /**
     * Risolve l’ente a partire dal code del criterio.
     * (in futuro: CriteriaCodeConfigs)
     */
    private Authority resolveAuthority(String code) {
        return switch (code) {
            case OnboardingConstants.CRITERIA_CODE_ISEE -> Authority.INPS;
            case OnboardingConstants.CRITERIA_CODE_RESIDENCE,
                    OnboardingConstants.CRITERIA_CODE_BIRTHDATE -> Authority.ANPR;
            default -> throw new IllegalArgumentException(
                    "Unsupported criteria code: " + code
            );
        };
    }

    /**
     * Invoca il servizio PDND e traduce il risultato in:
     *  - true  → verifica OK
     *  - false → verifica KO
     *  - exception → retry (PdndException)
     */
    private Mono<Boolean> invokeAuthority(
            Authority authority,
            VerifyDTO verify,
            OnboardingDTO onboardingRequest,
            String fiscalCode) {

        Mono<Optional<List<OnboardingRejectionReason>>> call =
                switch (authority) {

                    case INPS -> verify.getThresholdCode() == null
                            ? inpsDataRetrieverService.invoke(
                            fiscalCode,
                            pagoPaAnprPdndConfig
                                    .getPagopaPdndConfiguration()
                                    .get("c001"),
                            buildInvocation(verify),
                            onboardingRequest
                    )
                            : inpsThresholdRetrieverService.invoke(
                            fiscalCode,
                            pagoPaAnprPdndConfig
                                    .getPagopaPdndConfiguration()
                                    .get("c001"),
                            buildInvocation(verify),
                            onboardingRequest
                    );

                    case ANPR -> anprDataRetrieverService.invoke(
                            fiscalCode,
                            pagoPaAnprPdndConfig
                                    .getPagopaPdndConfiguration()
                                    .get("c001"),
                            buildInvocation(verify),
                            onboardingRequest
                    );
                };

        return call.map(optionalRejections -> {

            // ⏳ dato non disponibile → retry
            if (optionalRejections.isEmpty()) {
                throw new PdndException();
            }

            // ✅ lista vuota → verifica superata
            if (optionalRejections.get().isEmpty()) {
                return true;
            }

            // ❌ lista non vuota → verifica fallita
            return false;
        });
    }

    /**
     * Costruisce PdndServicesInvocation a partire dalla VerifyDTO
     * (wrapper minimo, senza logica di dominio)
     */

    private PdndServicesInvocation buildInvocation(VerifyDTO verify) {
        return new PdndServicesInvocation(
                verify.getCode(),
                verify.isVerify(),
                verify.getThresholdCode()
        );
    }


    private enum Authority {
        INPS,
        ANPR
    }
}