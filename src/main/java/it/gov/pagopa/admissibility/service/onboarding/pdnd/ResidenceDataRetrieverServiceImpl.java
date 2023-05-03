package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.connector.rest.anpr.exception.AnprDailyRequestLimitException;
import it.gov.pagopa.admissibility.connector.rest.anpr.residence.ResidenceAssessmentRestClient;
import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoGeneralitaDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoResidenzaDTO;
import it.gov.pagopa.admissibility.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
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
public class ResidenceDataRetrieverServiceImpl implements ResidenceDataRetrieverService {

    private final ResidenceAssessmentRestClient residenceAssessmentRestClient;
    private final CriteriaCodeService criteriaCodeService;
    private final TipoResidenzaDTO2ResidenceMapper residenceMapper;

    public ResidenceDataRetrieverServiceImpl(ResidenceAssessmentRestClient residenceAssessmentRestClient, CriteriaCodeService criteriaCodeService, TipoResidenzaDTO2ResidenceMapper residenceMapper) {
        this.residenceAssessmentRestClient = residenceAssessmentRestClient;
        this.criteriaCodeService = criteriaCodeService;
        this.residenceMapper = residenceMapper;
    }

    @Override
    public Mono<Optional<RispostaE002OKDTO>> invoke(String accessToken, String fiscalCode, AgidJwtTokenPayload agidJwtTokenPayload) {
        return residenceAssessmentRestClient.getResidenceAssessment(accessToken, fiscalCode, agidJwtTokenPayload).map(Optional::of)
                .onErrorResume(AnprDailyRequestLimitException.class, e -> {
                    log.debug("[ONBOARDING_REQUEST][RESIDENCE_ASSESSMENT] Daily limit occurred when calling ANPR service", e);
                    // TODO Short circuit all calls on that date
                    return Mono.just(Optional.empty());
                });
    }

    @Override
    public List<OnboardingRejectionReason> extract(RispostaE002OKDTO anprResponse, boolean getResidence, boolean getBirthDate, OnboardingDTO onboardingRequest) {
        List<OnboardingRejectionReason> rejectionReasons = new ArrayList<>();
        if(getResidence || getBirthDate) {
            TipoResidenzaDTO residence = null;
            TipoGeneralitaDTO personalInfo = null;

            if (anprResponse != null) {
                if (checkResidenceDataPresence(anprResponse)) {
                    //noinspection ConstantConditions: all condition are checked
                    residence = anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getResidenza().get(0);
                }
                if (checkPersonalInfoPresence(anprResponse)) {
                    //noinspection ConstantConditions: all condition are checked
                    personalInfo = anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita();
                }
            }

            extractResidenceData(getResidence, residence, onboardingRequest, rejectionReasons);
            extractBirthdateData(getBirthDate, personalInfo, onboardingRequest, rejectionReasons);
        }

        return rejectionReasons;
    }

    private boolean checkResidenceDataPresence(RispostaE002OKDTO anprResponse) {
        //noinspection ConstantConditions: all condition are checked
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
