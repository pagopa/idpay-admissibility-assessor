package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.*;
import it.gov.pagopa.admissibility.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.Order;
import it.gov.pagopa.admissibility.service.pdnd.CreateTokenService;
import it.gov.pagopa.admissibility.service.pdnd.UserFiscalCodeService;
import it.gov.pagopa.admissibility.service.pdnd.residence.ResidenceAssessmentService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class AuthoritiesDataRetrieverServiceImplTest {

    private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    private static final String FISCAL_CODE = "FISCAL_CODE";

    @Mock
    private OnboardingContextHolderService onboardingContextHolderServiceMock;

    @Mock
    private CreateTokenService createTokenServiceMock;
    @Mock
    private UserFiscalCodeService userFiscalCodeServiceMock;
    @Mock
    private ResidenceAssessmentService residenceAssessmentServiceMock;
    private final TipoResidenzaDTO2ResidenceMapper tipoResidenzaDTO2ResidenceMapper = new TipoResidenzaDTO2ResidenceMapper();

    private AuthoritiesDataRetrieverService authoritiesDataRetrieverService;

    private OnboardingDTO onboardingDTO;
    private InitiativeConfig initiativeConfig;
    private Message<String> message;

    @BeforeEach
    void setUp() {
        authoritiesDataRetrieverService = new AuthoritiesDataRetrieverServiceImpl(null, 60L, false, createTokenServiceMock, userFiscalCodeServiceMock, residenceAssessmentServiceMock, tipoResidenzaDTO2ResidenceMapper);

        onboardingDTO = OnboardingDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVEID")
                .tc(true)
                .status("STATUS")
                .pdndAccept(true)
                .tcAcceptTimestamp(LocalDateTime.of(2022,10,2,10,0,0))
                .criteriaConsensusTimestamp(LocalDateTime.of(2022,10,2,10,0,0))
                .build();

        LocalDate now = LocalDate.now();
        initiativeConfig = InitiativeConfig.builder()
                .initiativeId("INITIATIVEID")
                .initiativeName("INITITIATIVE_NAME")
                .organizationId("ORGANIZATIONID")
                .status("STATUS")
                .startDate(now)
                .endDate(now)
                .pdndToken("PDND_TOKEN")
                .initiativeBudget(new BigDecimal("100"))
                .beneficiaryInitiativeBudget(BigDecimal.TEN)
                .rankingInitiative(Boolean.TRUE)
                .build();

        RispostaE002OKDTO anprAnswer = buildAnprAnswer();

        message = MessageBuilder.withPayload(TestUtils.jsonSerializer(onboardingDTO)).build();

        Mockito.when(createTokenServiceMock.getToken(initiativeConfig.getPdndToken())).thenReturn(Mono.just(ACCESS_TOKEN));
        Mockito.when(userFiscalCodeServiceMock.getUserFiscalCode(onboardingDTO.getUserId())).thenReturn(Mono.just(FISCAL_CODE));
        Mockito.lenient().when(residenceAssessmentServiceMock.getResidenceAssessment(ACCESS_TOKEN, FISCAL_CODE)).thenReturn(Mono.just(anprAnswer));
    }

    @Test
    void retrieveIseeAutomatedCriteriaAndRanking() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("ISEE", "RESIDENCE", "BIRTHDATE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()));

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(BigDecimal.TEN, result.getIsee()); // TODO

        Residence expectedResidence = Residence.builder().city("Milano").province("MI").postalCode("20173").build();
        Assertions.assertEquals(expectedResidence, result.getResidence());

        BirthDate expectedBirthDate = BirthDate.builder().year("2001").age(21).build();
        Assertions.assertEquals(expectedBirthDate, result.getBirthDate());

        //TODO verify call INPS
        Mockito.verify(residenceAssessmentServiceMock).getResidenceAssessment(ACCESS_TOKEN, FISCAL_CODE);
    }

    @Test
    void retrieveIseeAutomatedCriteriaAndNotRanking() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("ISEE", "RESIDENCE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build()));

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(BigDecimal.TEN, result.getIsee());

        Residence expectedResidence = Residence.builder().city("Milano").province("MI").postalCode("20173").build();
        Assertions.assertEquals(expectedResidence, result.getResidence());

        Assertions.assertNull(result.getBirthDate());

        // TODO verify call INPS
        Mockito.verify(residenceAssessmentServiceMock).getResidenceAssessment(ACCESS_TOKEN, FISCAL_CODE);
    }

    @Test
    void retrieveResidenceNotAutomatedCriteriaNotRanking() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("RESIDENCE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build()));

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getIsee());

        Residence expectedResidence = Residence.builder().city("Milano").province("MI").postalCode("20173").build();
        Assertions.assertEquals(expectedResidence, result.getResidence());

        Assertions.assertNull(result.getBirthDate());

        // TODO verify not call INPS
        Mockito.verify(residenceAssessmentServiceMock).getResidenceAssessment(ACCESS_TOKEN, FISCAL_CODE);
    }

    @Test
    void retrieveIseeRankingAndNotAutomatedCriteria() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("RESIDENCE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build(),
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()

        ));

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(BigDecimal.TEN, result.getIsee());

        Residence expectedResidence = Residence.builder().city("Milano").province("MI").postalCode("20173").build();
        Assertions.assertEquals(expectedResidence, result.getResidence());

        Assertions.assertNull(result.getBirthDate());

        // TODO verify call INPS
        Mockito.verify(residenceAssessmentServiceMock).getResidenceAssessment(ACCESS_TOKEN, FISCAL_CODE);
    }

    @Test
    void retrieveIseeNotAutomatedCriteriaNotRanking() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("ISEE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()));

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(BigDecimal.TEN, result.getIsee());

        Assertions.assertNull(result.getResidence());
        Assertions.assertNull(result.getBirthDate());

        // TODO verify call INPS
        Mockito.verify(residenceAssessmentServiceMock, Mockito.never()).getResidenceAssessment(ACCESS_TOKEN, FISCAL_CODE);
    }

    private RispostaE002OKDTO buildAnprAnswer() {

        return new RispostaE002OKDTO().listaSoggetti(buildListaSoggetti());
    }

    private TipoListaSoggettiDTO buildListaSoggetti() {
        TipoGeneralitaDTO generalita = new TipoGeneralitaDTO();
        generalita.setDataNascita("2001-02-04");
        generalita.setSenzaGiornoMese("2001");

        TipoComuneDTO comune = new TipoComuneDTO();
        comune.setNomeComune("Milano");
        comune.setSiglaProvinciaIstat("MI");

        TipoIndirizzoDTO indirizzo = new TipoIndirizzoDTO();
        indirizzo.setCap("20173");
        indirizzo.setComune(comune);

        TipoResidenzaDTO residenza = new TipoResidenzaDTO();
        residenza.setIndirizzo(indirizzo);

        TipoDatiSoggettiEnteDTO datiSoggetto = new TipoDatiSoggettiEnteDTO();
        datiSoggetto.setGeneralita(generalita);
        datiSoggetto.setResidenza(List.of(residenza));

        TipoListaSoggettiDTO listaSoggetti = new TipoListaSoggettiDTO();
        listaSoggetti.setDatiSoggetto(List.of(datiSoggetto));

        return listaSoggetti;
    }
}