package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.rest.anpr.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.dto.anpr.response.PdndResponseBase;
import it.gov.pagopa.admissibility.dto.anpr.response.PdndResponseVisitor;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaKODTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoGeneralitaDTO;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.reactive.pdnd.exception.PdndServiceTooManyRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AnprDataRetrieverServiceImpl implements AnprDataRetrieverService {

    private final AnprC001RestClient anprC001RestClient;
    private final CriteriaCodeService criteriaCodeService;
    private final TipoResidenzaDTO2ResidenceMapper residenceMapper;

    public AnprDataRetrieverServiceImpl(
            AnprC001RestClient anprC001RestClient,
            CriteriaCodeService criteriaCodeService,
            TipoResidenzaDTO2ResidenceMapper residenceMapper) {

        this.anprC001RestClient = anprC001RestClient;
        this.criteriaCodeService = criteriaCodeService;
        this.residenceMapper = residenceMapper;
    }

    @Override
    public Mono<Optional<List<OnboardingRejectionReason>>> invoke(
            String fiscalCode,
            PdndInitiativeConfig pdndInitiativeConfig,
            PdndServicesInvocation invocation,
            OnboardingDTO onboardingRequest) {

        //  ANPR rilevante solo per alcuni code
        if (!invocation.requirePdndInvocation()
                || !(OnboardingConstants.CRITERIA_CODE_RESIDENCE.equalsIgnoreCase(invocation.getCode())
                || OnboardingConstants.CRITERIA_CODE_BIRTHDATE.equalsIgnoreCase(invocation.getCode()))) {

            return MONO_OPTIONAL_EMPTY_LIST;
        }

        return anprC001RestClient
                .invoke(fiscalCode, pdndInitiativeConfig)
                .map(response ->
                        Optional.of(
                                extractData(response, invocation.getCode(), onboardingRequest)
                        )
                )
                .onErrorResume(PdndServiceTooManyRequestException.class, e -> {
                    log.debug(
                            "[ONBOARDING_REQUEST][ANPR] Daily limit occurred when calling ANPR service",
                            e
                    );
                    return MONO_EMPTY_RESPONSE;
                });
    }

    /**
     * Una sola estrazione guidata dal code.
     */
    private List<OnboardingRejectionReason> extractData(
            PdndResponseBase<RispostaE002OKDTO, RispostaKODTO> response,
            String code,
            OnboardingDTO onboardingRequest) {

        List<OnboardingRejectionReason> rejectionReasons = new ArrayList<>(1);

        switch (code) {

            case OnboardingConstants.CRITERIA_CODE_RESIDENCE ->
                    extractResidenceData(response, onboardingRequest, rejectionReasons);

            case OnboardingConstants.CRITERIA_CODE_BIRTHDATE ->
                    extractBirthdateData(response, onboardingRequest, rejectionReasons);

            default -> {
                // ANPR non rilevante
            }
        }

        return rejectionReasons;
    }


    private void extractResidenceData(
            PdndResponseBase<RispostaE002OKDTO, RispostaKODTO> response,
            OnboardingDTO onboardingRequest,
            List<OnboardingRejectionReason> rejectionReasons) {

        onboardingRequest.setResidence(
                response.accept(new PdndResponseVisitor<>() {

                    @Override
                    public Residence onOk(RispostaE002OKDTO okdto) {
                        if (okdto != null && checkResidenceDataPresence(okdto)) {
                            return residenceMapper.apply(
                                    okdto.getListaSoggetti()
                                            .getDatiSoggetto()
                                            .get(0)
                                            .getResidenza()
                                            .get(0)
                            );
                        }
                        return null;
                    }

                    @Override
                    public Residence onKo(RispostaKODTO koDto) {
                        return null;
                    }
                })
        );

        if (onboardingRequest.getResidence() == null) {
            CriteriaCodeConfig cfg =
                    criteriaCodeService.getCriteriaCodeConfig(
                            OnboardingConstants.CRITERIA_CODE_RESIDENCE.toLowerCase()
                    );

            rejectionReasons.add(
                    new OnboardingRejectionReason(
                            OnboardingRejectionReason.OnboardingRejectionReasonType.RESIDENCE_KO,
                            OnboardingConstants.REJECTION_REASON_RESIDENCE_KO,
                            cfg.getAuthority(),
                            cfg.getAuthorityLabel(),
                            "Residenza non disponibile"
                    )
            );
        }
    }

    private boolean checkResidenceDataPresence(RispostaE002OKDTO anprResponse) {
        return anprResponse.getListaSoggetti() != null
                && !CollectionUtils.isEmpty(anprResponse.getListaSoggetti().getDatiSoggetto())
                && anprResponse.getListaSoggetti().getDatiSoggetto().get(0) != null
                && !CollectionUtils.isEmpty(
                anprResponse.getListaSoggetti()
                        .getDatiSoggetto()
                        .get(0)
                        .getResidenza()
        )
                && anprResponse.getListaSoggetti()
                .getDatiSoggetto()
                .get(0)
                .getResidenza()
                .get(0) != null;
    }



    private void extractBirthdateData(
            PdndResponseBase<RispostaE002OKDTO, RispostaKODTO> response,
            OnboardingDTO onboardingRequest,
            List<OnboardingRejectionReason> rejectionReasons) {

        onboardingRequest.setBirthDate(
                response.accept(new PdndResponseVisitor<>() {

                    @Override
                    public BirthDate onOk(RispostaE002OKDTO okdto) {
                        if (okdto != null && checkPersonalInfoPresence(okdto)) {
                            return getBirthDateFromAnpr(
                                    okdto.getListaSoggetti()
                                            .getDatiSoggetto()
                                            .get(0)
                                            .getGeneralita()
                            );
                        }
                        return null;
                    }

                    @Override
                    public BirthDate onKo(RispostaKODTO koDto) {
                        return null;
                    }
                })
        );

        if (onboardingRequest.getBirthDate() == null) {
            CriteriaCodeConfig cfg =
                    criteriaCodeService.getCriteriaCodeConfig(
                            OnboardingConstants.CRITERIA_CODE_BIRTHDATE.toLowerCase()
                    );

            rejectionReasons.add(
                    new OnboardingRejectionReason(
                            OnboardingRejectionReason.OnboardingRejectionReasonType.BIRTHDATE_KO,
                            OnboardingConstants.REJECTION_REASON_BIRTHDATE_KO,
                            cfg.getAuthority(),
                            cfg.getAuthorityLabel(),
                            "Data di nascita non disponibile"
                    )
            );
        }
    }

    private boolean checkPersonalInfoPresence(RispostaE002OKDTO anprResponse) {
        return anprResponse.getListaSoggetti() != null
                && !CollectionUtils.isEmpty(anprResponse.getListaSoggetti().getDatiSoggetto())
                && anprResponse.getListaSoggetti().getDatiSoggetto().get(0) != null
                && anprResponse.getListaSoggetti()
                .getDatiSoggetto()
                .get(0)
                .getGeneralita() != null;
    }

    private static final DateTimeFormatter DATA_NASCITA_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE;

    private BirthDate getBirthDateFromAnpr(TipoGeneralitaDTO personalInfo) {

        if (personalInfo.getDataNascita() == null) {
            return null;
        }

        LocalDate birthDate =
                LocalDate.parse(
                        personalInfo.getDataNascita(),
                        DATA_NASCITA_FORMATTER
                );

        return BirthDate.builder()
                .age(Period.between(birthDate, LocalDate.now()).getYears())
                .year(String.valueOf(birthDate.getYear()))
                .build();
    }
}