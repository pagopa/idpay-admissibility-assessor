package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoGeneralitaDTO;
import it.gov.pagopa.admissibility.mapper.TipoResidenzaDTO2ResidenceMapper;
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

    public static final Mono<Optional<List<OnboardingRejectionReason>>> MONO_EMPTY_RESPONSE = Mono.just(Optional.empty());

    private final AnprC001RestClient anprC001RestClient;
    private final CriteriaCodeService criteriaCodeService;
    private final TipoResidenzaDTO2ResidenceMapper residenceMapper;

    public AnprDataRetrieverServiceImpl(AnprC001RestClient anprC001RestClient, CriteriaCodeService criteriaCodeService, TipoResidenzaDTO2ResidenceMapper residenceMapper) {
        this.anprC001RestClient = anprC001RestClient;
        this.criteriaCodeService = criteriaCodeService;
        this.residenceMapper = residenceMapper;
    }

    public static boolean accept(PdndServicesInvocation pdndServicesInvocation) {
        return pdndServicesInvocation.isGetResidence() || pdndServicesInvocation.isGetBirthDate();
    }

    @Override
    public Mono<Optional<List<OnboardingRejectionReason>>> invoke(
            String fiscalCode,
            PdndInitiativeConfig pdndInitiativeConfig,
            PdndServicesInvocation pdndServicesInvocation, OnboardingDTO onboardingRequest) {
        if (!accept(pdndServicesInvocation)) {
            return MONO_OPTIONAL_EMPTY_LIST;
        }

        return anprC001RestClient.invoke(fiscalCode, pdndInitiativeConfig)
                .map(anprResponse -> Optional.of(extractData(anprResponse, pdndServicesInvocation, onboardingRequest)))

                .onErrorResume(PdndServiceTooManyRequestException.class, e -> {
                    log.debug("[ONBOARDING_REQUEST][RESIDENCE_ASSESSMENT] Daily limit occurred when calling ANPR service", e);
                    // TODO Short circuit all calls on that date for the same clientId
                    return MONO_EMPTY_RESPONSE;
                });
    }

    private List<OnboardingRejectionReason> extractData(RispostaE002OKDTO anprResponse, PdndServicesInvocation pdndServicesInvocation, OnboardingDTO onboardingRequest) {
        List<OnboardingRejectionReason> rejectionReasons = new ArrayList<>(2);

        if (pdndServicesInvocation.isGetResidence()) {
            extractResidenceData(anprResponse, onboardingRequest, rejectionReasons);
        }
        if (pdndServicesInvocation.isGetBirthDate()) {
            extractBirthdateData(anprResponse, onboardingRequest, rejectionReasons);
        }

        return rejectionReasons;
    }

    private boolean checkResidenceDataPresence(RispostaE002OKDTO anprResponse) {
        return anprResponse.getListaSoggetti() != null
                &&
                !CollectionUtils.isEmpty(anprResponse.getListaSoggetti().getDatiSoggetto())
                &&
                anprResponse.getListaSoggetti().getDatiSoggetto().get(0) != null
                &&
                !CollectionUtils.isEmpty(anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getResidenza())
                &&
                anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getResidenza().get(0) != null;
    }

    private void extractResidenceData(RispostaE002OKDTO anprResponse, OnboardingDTO onboardingRequest, List<OnboardingRejectionReason> rejectionReasons) {
        if (anprResponse != null && checkResidenceDataPresence(anprResponse)) {
            onboardingRequest.setResidence(residenceMapper.apply(anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getResidenza().get(0)));
        }

        if (onboardingRequest.getResidence() == null) {
            CriteriaCodeConfig criteriaCodeConfig = criteriaCodeService.getCriteriaCodeConfig(OnboardingConstants.CRITERIA_CODE_RESIDENCE);
            rejectionReasons.add(
                    new OnboardingRejectionReason(
                            OnboardingRejectionReason.OnboardingRejectionReasonType.RESIDENCE_KO,
                            OnboardingConstants.REJECTION_REASON_RESIDENCE_KO,
                            criteriaCodeConfig.getAuthority(),
                            criteriaCodeConfig.getAuthorityLabel(),
                            "Residenza non disponibile"
                    )
            );
        } else {
            // TODO log userId and residence obtained from ANPR
        }
    }

    private boolean checkPersonalInfoPresence(RispostaE002OKDTO anprResponse) {
        return anprResponse.getListaSoggetti() != null
                &&
                !CollectionUtils.isEmpty(anprResponse.getListaSoggetti().getDatiSoggetto())
                &&
                anprResponse.getListaSoggetti().getDatiSoggetto().get(0) != null
                &&
                anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita() != null;
    }

    private void extractBirthdateData(RispostaE002OKDTO anprResponse, OnboardingDTO onboardingRequest, List<OnboardingRejectionReason> rejectionReasons) {
        if (anprResponse != null && checkPersonalInfoPresence(anprResponse)) {
            onboardingRequest.setBirthDate(getBirthDateFromAnpr(anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita()));
        }

        if (onboardingRequest.getBirthDate() == null) {
            CriteriaCodeConfig criteriaCodeConfig = criteriaCodeService.getCriteriaCodeConfig(OnboardingConstants.CRITERIA_CODE_BIRTHDATE);
            rejectionReasons.add(
                    new OnboardingRejectionReason(
                            OnboardingRejectionReason.OnboardingRejectionReasonType.BIRTHDATE_KO,
                            OnboardingConstants.REJECTION_REASON_BIRTHDATE_KO,
                            criteriaCodeConfig.getAuthority(),
                            criteriaCodeConfig.getAuthorityLabel(),
                            "Data di nascita non disponibile"
                    )
            );
        } else {
            // TODO log userId and birth date obtained from ANPR
        }
    }

    private static final DateTimeFormatter dataNascitaFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    private BirthDate getBirthDateFromAnpr(TipoGeneralitaDTO personalInfo) {
        if (personalInfo.getDataNascita() != null) {
            LocalDate birthDate = LocalDate.parse(personalInfo.getDataNascita(), dataNascitaFormatter);

            return BirthDate.builder()
                    .age(Period.between(
                                    birthDate,
                                    LocalDate.now())
                            .getYears())
                    .year(birthDate.getYear() + "")
                    .build();
        } else {
            return null;
        }
    }
}
