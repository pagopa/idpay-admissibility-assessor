package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoGeneralitaDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoResidenzaDTO;
import it.gov.pagopa.admissibility.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.connector.rest.anpr.exception.AnprDailyRequestLimitException;
import it.gov.pagopa.admissibility.service.pdnd.residence.ResidenceAssessmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

@Service
@Slf4j
public class AnprInvocationServiceImpl implements AnprInvocationService {

    private final ResidenceAssessmentService residenceAssessmentService;
    private final TipoResidenzaDTO2ResidenceMapper residenceMapper;

    public AnprInvocationServiceImpl(ResidenceAssessmentService residenceAssessmentService, TipoResidenzaDTO2ResidenceMapper residenceMapper) {
        this.residenceAssessmentService = residenceAssessmentService;
        this.residenceMapper = residenceMapper;
    }

    @Override
    public Mono<Optional<RispostaE002OKDTO>> invoke(String accessToken, String fiscalCode, AgidJwtTokenPayload agidJwtTokenPayload) {
        return residenceAssessmentService.getResidenceAssessment(accessToken, fiscalCode, agidJwtTokenPayload).map(Optional::of)
                .onErrorResume(AnprDailyRequestLimitException.class, e -> {
                    log.debug("[ONBOARDING_REQUEST][RESIDENCE_ASSESSMENT] Daily limit occurred when calling ANPR service", e);
                    // TODO Short circuit all calls on that date
                    return Mono.just(Optional.empty());
                });
    }

    @Override
    public void extract(RispostaE002OKDTO anprResponse, boolean getResidence, boolean getBirthDate, OnboardingDTO onboardingRequest) {
        if (anprResponse != null) {
            TipoResidenzaDTO residence = new TipoResidenzaDTO();
            TipoGeneralitaDTO personalInfo = new TipoGeneralitaDTO();

            // TODO what to do if these don't exist?
            if (checkResidenceDataPresence(anprResponse))
                residence = anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getResidenza().get(0);
            if (checkPersonalInfoPresence(anprResponse))
                personalInfo = anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita();

            if (getResidence) {
                // TODO log userId and residence obtained from ANPR
                onboardingRequest.setResidence(residenceMapper.apply(residence));
            }
            if (getBirthDate) {
                // TODO log userId and birth date obtained from ANPR
                onboardingRequest.setBirthDate(getBirthDateFromAnpr(personalInfo));
            }

        }
    }

    private boolean checkResidenceDataPresence(RispostaE002OKDTO anprResponse) {
        return anprResponse.getListaSoggetti() != null
                &&
                anprResponse.getListaSoggetti().getDatiSoggetto() != null
                &&
                anprResponse.getListaSoggetti().getDatiSoggetto().get(0) != null
                &&
                anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getResidenza() != null
                &&
                anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getResidenza().get(0) != null;
    }

    private boolean checkPersonalInfoPresence(RispostaE002OKDTO anprResponse) {
        return anprResponse.getListaSoggetti() != null
                &&
                anprResponse.getListaSoggetti().getDatiSoggetto() != null
                &&
                anprResponse.getListaSoggetti().getDatiSoggetto().get(0) != null
                &&
                anprResponse.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita() != null;
    }

    private BirthDate getBirthDateFromAnpr(TipoGeneralitaDTO personalInfo) {
        return BirthDate.builder()
                .age(Period.between(
                                LocalDate.parse(personalInfo.getDataNascita()),
                                LocalDate.now())
                        .getYears())
                .year(personalInfo.getSenzaGiornoMese())
                .build();
    }
}
