package it.gov.pagopa.admissibility.connector.rest.anpr.service;

import it.gov.pagopa.common.reactive.pdnd.exception.PdndServiceTooManyRequestException;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoGeneralitaDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoResidenzaDTO;
import it.gov.pagopa.admissibility.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.service.onboarding.AuthoritiesDataRetrieverServiceImpl;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
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

    @Override
    public Mono<Optional<List<OnboardingRejectionReason>>> invoke(
            String fiscalCode,
            PdndInitiativeConfig pdndInitiativeConfig,
            AuthoritiesDataRetrieverServiceImpl.PdndServicesInvocation pdndServicesInvocation, OnboardingDTO onboardingRequest) {
        return anprC001RestClient.invoke(fiscalCode, pdndInitiativeConfig)
                .map(anprResponse -> Optional.of(extractData(anprResponse, pdndServicesInvocation, onboardingRequest)))

                .onErrorResume(PdndServiceTooManyRequestException.class, e -> {
                    log.debug("[ONBOARDING_REQUEST][RESIDENCE_ASSESSMENT] Daily limit occurred when calling ANPR service", e);
                    // TODO Short circuit all calls on that date for the same clientId
                    return MONO_EMPTY_RESPONSE;
                });
    }

    private List<OnboardingRejectionReason> extractData(RispostaE002OKDTO anprResponse, AuthoritiesDataRetrieverServiceImpl.PdndServicesInvocation pdndServicesInvocation, OnboardingDTO onboardingRequest) {
        boolean getResidence=pdndServicesInvocation.isGetResidence();
        boolean getBirthDate= pdndServicesInvocation.isGetBirthDate();

        List<OnboardingRejectionReason> rejectionReasons = new ArrayList<>();
        if(getResidence || getBirthDate) {
            TipoResidenzaDTO residence = null;
            TipoGeneralitaDTO personalInfo = null;

            if (anprResponse != null) {
                if (checkResidenceDataPresence(anprResponse)) {
                    residence = anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getResidenza().get(0);
                }
                if (checkPersonalInfoPresence(anprResponse)) {
                    personalInfo = anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita();
                }
            }

            extractResidenceData(getResidence, residence, onboardingRequest, rejectionReasons);
            extractBirthdateData(getBirthDate, personalInfo, onboardingRequest, rejectionReasons);
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

    private boolean checkPersonalInfoPresence(RispostaE002OKDTO anprResponse) {
        return anprResponse.getListaSoggetti() != null
                &&
                !CollectionUtils.isEmpty(anprResponse.getListaSoggetti().getDatiSoggetto())
                &&
                anprResponse.getListaSoggetti().getDatiSoggetto().get(0) != null
                &&
                anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita() != null;
    }

    private void extractResidenceData(boolean getResidence, TipoResidenzaDTO residence, OnboardingDTO onboardingRequest, List<OnboardingRejectionReason> rejectionReasons) {
        if (getResidence) {
            if (residence != null) {
                onboardingRequest.setResidence(residenceMapper.apply(residence));
                // TODO log userId and residence obtained from ANPR
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
            }
        }
    }

    private void extractBirthdateData(boolean getBirthDate, TipoGeneralitaDTO personalInfo, OnboardingDTO onboardingRequest, List<OnboardingRejectionReason> rejectionReasons) {
        if (getBirthDate) {
            if(personalInfo != null) {
                onboardingRequest.setBirthDate(getBirthDateFromAnpr(personalInfo));
                // TODO log userId and birth date obtained from ANPR
            }

            if(onboardingRequest.getBirthDate()==null){
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
            }
        }
    }

    private static final DateTimeFormatter dataNascitaFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private BirthDate getBirthDateFromAnpr(TipoGeneralitaDTO personalInfo) {
        if(personalInfo.getDataNascita()!=null) {
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
