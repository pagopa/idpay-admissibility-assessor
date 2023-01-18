package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.*;
import it.gov.pagopa.admissibility.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.Order;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.AnprInvocationService;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.PdndInvocationsUtils;
import it.gov.pagopa.admissibility.service.pdnd.CreateTokenService;
import it.gov.pagopa.admissibility.service.pdnd.UserFiscalCodeService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthoritiesDataRetrieverServiceImplTest {

    private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    private static final String FISCAL_CODE = "FISCAL_CODE";
    public static final String API_KEY_CLIENT_ID = "API_KEY_CLIENT_ID";
    public static final String API_KEY_CLIENT_ASSERTION = "API_KEY_CLIENT_ASSERTION";

    @Mock
    private OnboardingContextHolderService onboardingContextHolderServiceMock;

    @Mock
    private CreateTokenService createTokenServiceMock;
    @Mock
    private UserFiscalCodeService userFiscalCodeServiceMock;
    @Spy
    private AnprInvocationService anprInvocationServiceSpy;
    @Mock
    private StreamBridge streamBridgeMock;

    private final TipoResidenzaDTO2ResidenceMapper residenceMapper = new TipoResidenzaDTO2ResidenceMapper();

    private AuthoritiesDataRetrieverService authoritiesDataRetrieverService;

    private OnboardingDTO onboardingDTO;
    private InitiativeConfig initiativeConfig;
    private RispostaE002OKDTO anprAnswer;
    private Message<String> message;
    private ApiKeysPDND apiKeysPDND;

    @BeforeEach
    void setUp() {
        authoritiesDataRetrieverService = new AuthoritiesDataRetrieverServiceImpl(streamBridgeMock, 60L, false, createTokenServiceMock, userFiscalCodeServiceMock, anprInvocationServiceSpy, onboardingContextHolderServiceMock);

        onboardingDTO = OnboardingDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVEID")
                .tc(true)
                .status("STATUS")
                .pdndAccept(true)
                .tcAcceptTimestamp(LocalDateTime.of(2022, 10, 2, 10, 0, 0))
                .criteriaConsensusTimestamp(LocalDateTime.of(2022, 10, 2, 10, 0, 0))
                .build();

        LocalDate now = LocalDate.now();
        initiativeConfig = InitiativeConfig.builder()
                .initiativeId("INITIATIVEID")
                .initiativeName("INITITIATIVE_NAME")
                .organizationId("ORGANIZATIONID")
                .status("STATUS")
                .startDate(now)
                .endDate(now)
                .apiKeyClientId(API_KEY_CLIENT_ID)
                .apiKeyClientAssertion(API_KEY_CLIENT_ASSERTION)
                .initiativeBudget(new BigDecimal("100"))
                .beneficiaryInitiativeBudget(BigDecimal.TEN)
                .rankingInitiative(Boolean.TRUE)
                .build();

        apiKeysPDND = ApiKeysPDND.builder()
                .apiKeyClientId(API_KEY_CLIENT_ID)
                .apiKeyClientAssertion(API_KEY_CLIENT_ASSERTION)
                .build();

        anprAnswer = PdndInvocationsUtils.buildAnprAnswer();

        message = MessageBuilder.withPayload(TestUtils.jsonSerializer(onboardingDTO)).build();

        Mockito.when(onboardingContextHolderServiceMock.getPDNDapiKeys(initiativeConfig)).thenReturn(apiKeysPDND);
        Mockito.when(createTokenServiceMock.getToken(apiKeysPDND)).thenReturn(Mono.just(ACCESS_TOKEN));
        Mockito.when(userFiscalCodeServiceMock.getUserFiscalCode(onboardingDTO.getUserId())).thenReturn(Mono.just(FISCAL_CODE));
    }

    @Test
    void retrieveIseeAutomatedCriteriaAndRanking() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("ISEE", "RESIDENCE", "BIRTHDATE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()));

        Residence expectedResidence = Residence.builder().city("Milano").province("MI").postalCode("20143").build();
        BirthDate expectedBirthDate = BirthDate.builder().year("2001").age(21).build();

        Mockito.when(anprInvocationServiceSpy.invoke(ACCESS_TOKEN, FISCAL_CODE)).thenReturn(Mono.just(Optional.of(anprAnswer)));
        Mockito.doAnswer(i -> {
            // TODO ISEE
            onboardingDTO.setResidence(residenceMapper.apply(getResidenceFromAnswer(anprAnswer)));
            onboardingDTO.setBirthDate(getBirthDateFromAnswer(anprAnswer));
            return null;
        }).when(anprInvocationServiceSpy).extract(anprAnswer, true, true, onboardingDTO);

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new BigDecimal("74585"), result.getIsee()); // TODO

        Assertions.assertEquals(expectedResidence, result.getResidence());

        Assertions.assertEquals(expectedBirthDate, result.getBirthDate());

        //TODO verify call INPS
        Mockito.verify(anprInvocationServiceSpy).invoke(ACCESS_TOKEN, FISCAL_CODE);
    }

    @Test
    void retrieveIseeAutomatedCriteriaAndNotRanking() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("ISEE", "RESIDENCE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build()));

        Mockito.when(anprInvocationServiceSpy.invoke(ACCESS_TOKEN, FISCAL_CODE)).thenReturn(Mono.just(Optional.of(anprAnswer)));
        Mockito.doAnswer(i -> {
            // TODO ISEE
            onboardingDTO.setResidence(residenceMapper.apply(getResidenceFromAnswer(anprAnswer)));
            return null;
        }).when(anprInvocationServiceSpy).extract(anprAnswer, true, false, onboardingDTO);

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new BigDecimal("74585"), result.getIsee());

        Residence expectedResidence = Residence.builder().city("Milano").province("MI").postalCode("20143").build();
        Assertions.assertEquals(expectedResidence, result.getResidence());

        Assertions.assertNull(result.getBirthDate());

        // TODO verify call INPS
        Mockito.verify(anprInvocationServiceSpy).invoke(ACCESS_TOKEN, FISCAL_CODE);
    }

    @Test
    void retrieveResidenceNotAutomatedCriteriaNotRanking() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("RESIDENCE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build()));

        Mockito.when(anprInvocationServiceSpy.invoke(ACCESS_TOKEN, FISCAL_CODE)).thenReturn(Mono.just(Optional.of(anprAnswer)));
        Mockito.doAnswer(i -> {
            onboardingDTO.setResidence(residenceMapper.apply(getResidenceFromAnswer(anprAnswer)));
            return null;
        }).when(anprInvocationServiceSpy).extract(anprAnswer, true, false, onboardingDTO);

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getIsee());

        Residence expectedResidence = Residence.builder().city("Milano").province("MI").postalCode("20143").build();
        Assertions.assertEquals(expectedResidence, result.getResidence());

        Assertions.assertNull(result.getBirthDate());

        // TODO verify not call INPS
        Mockito.verify(anprInvocationServiceSpy).invoke(ACCESS_TOKEN, FISCAL_CODE);
    }

    @Test
    void retrieveIseeRankingAndNotAutomatedCriteria() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("RESIDENCE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build(),
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()));

        Mockito.when(anprInvocationServiceSpy.invoke(ACCESS_TOKEN, FISCAL_CODE)).thenReturn(Mono.just(Optional.of(anprAnswer)));
        Mockito.doAnswer(i -> {
            // TODO ISEE
            onboardingDTO.setResidence(residenceMapper.apply(getResidenceFromAnswer(anprAnswer)));
            return null;
        }).when(anprInvocationServiceSpy).extract(anprAnswer, true, false, onboardingDTO);

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new BigDecimal("74585"), result.getIsee());

        Residence expectedResidence = Residence.builder().city("Milano").province("MI").postalCode("20143").build();
        Assertions.assertEquals(expectedResidence, result.getResidence());

        Assertions.assertNull(result.getBirthDate());

        // TODO verify call INPS
        Mockito.verify(anprInvocationServiceSpy).invoke(ACCESS_TOKEN, FISCAL_CODE);
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
        Assertions.assertEquals(new BigDecimal("74585"), result.getIsee());

        Assertions.assertNull(result.getResidence());
        Assertions.assertNull(result.getBirthDate());

        // TODO verify call INPS
        Mockito.verify(anprInvocationServiceSpy, Mockito.never()).invoke(ACCESS_TOKEN, FISCAL_CODE);
    }

    @Test
    void testDailyLimitReached() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("ISEE", "RESIDENCE", "BIRTHDATE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()));

        Mockito.when(anprInvocationServiceSpy.invoke(ACCESS_TOKEN, FISCAL_CODE)).thenReturn(Mono.just(Optional.empty()));
        Mockito.when(streamBridgeMock.send(Mockito.anyString(), Mockito.any())).thenReturn(true);

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        // Then
        Assertions.assertNull(result);

        // TODO verify call INPS
        Mockito.verify(anprInvocationServiceSpy).invoke(ACCESS_TOKEN, FISCAL_CODE);
        Mockito.verify(streamBridgeMock).send(Mockito.anyString(), Mockito.any());
    }

    private TipoResidenzaDTO getResidenceFromAnswer(RispostaE002OKDTO anprAnswer) {
        return anprAnswer.getListaSoggetti().getDatiSoggetto().get(0).getResidenza().get(0);
    }

    private BirthDate getBirthDateFromAnswer(RispostaE002OKDTO anprAnswer) {
        String year = anprAnswer.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita().getSenzaGiornoMese();
        Integer age = Period.between(
                        LocalDate.parse(anprAnswer.getListaSoggetti().getDatiSoggetto().get(0).getGeneralita().getDataNascita()),
                        LocalDate.now())
                .getYears();

        return new BirthDate(year, age);
    }
}