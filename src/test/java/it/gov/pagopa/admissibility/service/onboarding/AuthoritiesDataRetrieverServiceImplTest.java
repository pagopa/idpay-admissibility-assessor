package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.dto.in_memory.AgidJwtTokenPayload;
import it.gov.pagopa.admissibility.dto.in_memory.ApiKeysPDND;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.EsitoEnum;
import it.gov.pagopa.admissibility.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.model.Order;
import it.gov.pagopa.admissibility.service.onboarding.notifier.OnboardingRescheduleService;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.AnprInvocationService;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.InpsInvocationService;
import it.gov.pagopa.admissibility.service.onboarding.pdnd.PdndInvocationsTestUtils;
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
import org.springframework.data.domain.Sort;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthoritiesDataRetrieverServiceImplTest {

    private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    private static final String FISCAL_CODE = "FISCAL_CODE";
    public static final String ENCRYPTED_API_KEY_CLIENT_ID = "a5vd3W7VnhR5Sv44qxgXonZIlMAX9cWnCRiQq5h8";
    public static final String ENCRYPTED_API_KEY_CLIENT_ASSERTION = "a5vd3W7VnhR5Sv44ow+VbR5Rq7pMHG/U2PhWdEnzWPx5gHYqhA";
    public static final String DECRYPTED_API_KEY_CLIENT_ID = "DECRYPTED_API_KEY_CLIENT_ID";
    public static final String DECRYPTED_API_KEY_CLIENT_ASSERTION = "DECRYPTED_API_KEY_CLIENT_ASSERTION";
    public static final IseeTypologyEnum ISEE_TYPOLOGY = IseeTypologyEnum.UNIVERSITARIO;

    private final OffsetDateTime TEST_DATE_TIME = OffsetDateTime.now();

    @Mock
    private OnboardingContextHolderService onboardingContextHolderServiceMock;

    @Mock
    private CreateTokenService createTokenServiceMock;
    @Mock
    private UserFiscalCodeService userFiscalCodeServiceMock;
    @Spy
    private AnprInvocationService anprInvocationServiceSpy;
    @Spy
    private InpsInvocationService inpsInvocationServiceSpy;
    @Mock
    private OnboardingRescheduleService onboardingRescheduleServiceMock;

    private final TipoResidenzaDTO2ResidenceMapper residenceMapper = new TipoResidenzaDTO2ResidenceMapper();

    private AuthoritiesDataRetrieverService authoritiesDataRetrieverService;

    private OnboardingDTO onboardingDTO;
    private InitiativeConfig initiativeConfig;
    private ConsultazioneIndicatoreResponseType inpsResponse;
    private RispostaE002OKDTO anprResponse;
    private Message<String> message;
    private AgidJwtTokenPayload agidTokenPayload;

    @BeforeEach
    void setUp() throws JAXBException {
        authoritiesDataRetrieverService = new AuthoritiesDataRetrieverServiceImpl(60L, false, onboardingRescheduleServiceMock, createTokenServiceMock, userFiscalCodeServiceMock, inpsInvocationServiceSpy, anprInvocationServiceSpy, onboardingContextHolderServiceMock);

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
        List<IseeTypologyEnum> typology = List.of(IseeTypologyEnum.UNIVERSITARIO, IseeTypologyEnum.ORDINARIO);
        initiativeConfig = InitiativeConfig.builder()
                .initiativeId("INITIATIVEID")
                .initiativeName("INITITIATIVE_NAME")
                .organizationId("ORGANIZATIONID")
                .status("STATUS")
                .startDate(now)
                .endDate(now)
                .apiKeyClientId(ENCRYPTED_API_KEY_CLIENT_ID)
                .apiKeyClientAssertion(ENCRYPTED_API_KEY_CLIENT_ASSERTION)
                .initiativeBudget(new BigDecimal("100"))
                .beneficiaryInitiativeBudget(BigDecimal.TEN)
                .rankingInitiative(Boolean.TRUE)
                .automatedCriteria(List.of(new AutomatedCriteriaDTO("AUTH1", "ISEE", null, FilterOperator.EQ, "1", null, Sort.Direction.ASC, typology)))
                .build();

        agidTokenPayload = AgidJwtTokenPayload.builder().iss("ISS").sub("SUB").aud("AUD").build();
        ApiKeysPDND apiKeysPDND = ApiKeysPDND.builder()
                .apiKeyClientId(DECRYPTED_API_KEY_CLIENT_ID)
                .apiKeyClientAssertion(DECRYPTED_API_KEY_CLIENT_ASSERTION)
                .agidJwtTokenPayload(agidTokenPayload)
                .build();

        inpsResponse = PdndInvocationsTestUtils.buildInpsResponse(EsitoEnum.OK);

        anprResponse = PdndInvocationsTestUtils.buildAnprResponse();

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
        BirthDate expectedBirthDate = BirthDate.builder().year("2001").age(LocalDate.now().getYear() - 2001).build();

        Mockito.when(inpsInvocationServiceSpy.invoke(FISCAL_CODE, ISEE_TYPOLOGY)).thenReturn(Mono.just(Optional.of(inpsResponse)));
        Mockito.doAnswer(i -> {
            onboardingDTO.setIsee(PdndInvocationsTestUtils.getIseeFromResponse(inpsResponse));
            return null;
        }).when(inpsInvocationServiceSpy).extract(inpsResponse, true, onboardingDTO);

        Mockito.when(anprInvocationServiceSpy.invoke(ACCESS_TOKEN, FISCAL_CODE, agidTokenPayload)).thenReturn(Mono.just(Optional.of(anprResponse)));
        Mockito.doAnswer(i -> {
            onboardingDTO.setResidence(residenceMapper.apply(PdndInvocationsTestUtils.getResidenceFromAnswer(anprResponse)));
            onboardingDTO.setBirthDate(PdndInvocationsTestUtils.getBirthDateFromAnswer(anprResponse));
            return null;
        }).when(anprInvocationServiceSpy).extract(anprResponse, true, true, onboardingDTO);

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new BigDecimal("10000"), result.getIsee());
        Assertions.assertEquals(expectedResidence, result.getResidence());
        Assertions.assertEquals(expectedBirthDate, result.getBirthDate());

        Mockito.verify(inpsInvocationServiceSpy).invoke(FISCAL_CODE, ISEE_TYPOLOGY);
        Mockito.verify(anprInvocationServiceSpy).invoke(ACCESS_TOKEN, FISCAL_CODE, agidTokenPayload);
    }

    @Test
    void retrieveIseeAutomatedCriteriaAndNotRanking() {
        // Given
        Residence expectedResidence = Residence.builder().city("Milano").province("MI").postalCode("20143").build();

        initiativeConfig.setAutomatedCriteriaCodes(List.of("ISEE", "RESIDENCE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build()));

        Mockito.when(inpsInvocationServiceSpy.invoke(FISCAL_CODE, ISEE_TYPOLOGY)).thenReturn(Mono.just(Optional.of(inpsResponse)));
        Mockito.doAnswer(i -> {
            onboardingDTO.setIsee(PdndInvocationsTestUtils.getIseeFromResponse(inpsResponse));
            return null;
        }).when(inpsInvocationServiceSpy).extract(inpsResponse, true, onboardingDTO);

        Mockito.when(anprInvocationServiceSpy.invoke(ACCESS_TOKEN, FISCAL_CODE, agidTokenPayload)).thenReturn(Mono.just(Optional.of(anprResponse)));
        Mockito.doAnswer(i -> {
            onboardingDTO.setResidence(residenceMapper.apply(PdndInvocationsTestUtils.getResidenceFromAnswer(anprResponse)));
            return null;
        }).when(anprInvocationServiceSpy).extract(anprResponse, true, false, onboardingDTO);

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new BigDecimal("10000"), result.getIsee());
        Assertions.assertEquals(expectedResidence, result.getResidence());
        Assertions.assertNull(result.getBirthDate());

        Mockito.verify(inpsInvocationServiceSpy).invoke(FISCAL_CODE, ISEE_TYPOLOGY);
        Mockito.verify(anprInvocationServiceSpy).invoke(ACCESS_TOKEN, FISCAL_CODE, agidTokenPayload);
    }

    @Test
    void retrieveResidenceNotAutomatedCriteriaNotRanking() {
        // Given
        Residence expectedResidence = Residence.builder().city("Milano").province("MI").postalCode("20143").build();

        initiativeConfig.setAutomatedCriteriaCodes(List.of("RESIDENCE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build()));

        Mockito.when(anprInvocationServiceSpy.invoke(ACCESS_TOKEN, FISCAL_CODE, agidTokenPayload)).thenReturn(Mono.just(Optional.of(anprResponse)));
        Mockito.doAnswer(i -> {
            onboardingDTO.setResidence(residenceMapper.apply(PdndInvocationsTestUtils.getResidenceFromAnswer(anprResponse)));
            return null;
        }).when(anprInvocationServiceSpy).extract(anprResponse, true, false, onboardingDTO);

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getIsee());
        Assertions.assertEquals(expectedResidence, result.getResidence());
        Assertions.assertNull(result.getBirthDate());

        Mockito.verify(inpsInvocationServiceSpy, Mockito.never()).invoke(FISCAL_CODE, ISEE_TYPOLOGY);
        Mockito.verify(anprInvocationServiceSpy).invoke(ACCESS_TOKEN, FISCAL_CODE, agidTokenPayload);
    }

    @Test
    void retrieveIseeRankingAndNotAutomatedCriteria() {
        // Given
        Residence expectedResidence = Residence.builder().city("Milano").province("MI").postalCode("20143").build();

        initiativeConfig.setAutomatedCriteriaCodes(List.of("RESIDENCE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build(),
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()));

        Mockito.when(inpsInvocationServiceSpy.invoke(FISCAL_CODE, ISEE_TYPOLOGY)).thenReturn(Mono.just(Optional.of(inpsResponse)));
        Mockito.doAnswer(i -> {
            onboardingDTO.setIsee(PdndInvocationsTestUtils.getIseeFromResponse(inpsResponse));
            return null;
        }).when(inpsInvocationServiceSpy).extract(inpsResponse, true, onboardingDTO);

        Mockito.when(anprInvocationServiceSpy.invoke(ACCESS_TOKEN, FISCAL_CODE, agidTokenPayload)).thenReturn(Mono.just(Optional.of(anprResponse)));
        Mockito.doAnswer(i -> {
            onboardingDTO.setResidence(residenceMapper.apply(PdndInvocationsTestUtils.getResidenceFromAnswer(anprResponse)));
            return null;
        }).when(anprInvocationServiceSpy).extract(anprResponse, true, false, onboardingDTO);

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new BigDecimal("10000"), result.getIsee());
        Assertions.assertEquals(expectedResidence, result.getResidence());
        Assertions.assertNull(result.getBirthDate());

        Mockito.verify(inpsInvocationServiceSpy).invoke(FISCAL_CODE, ISEE_TYPOLOGY);
        Mockito.verify(anprInvocationServiceSpy).invoke(ACCESS_TOKEN, FISCAL_CODE, agidTokenPayload);
    }

    @Test
    void retrieveIseeNotAutomatedCriteriaNotRanking() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("ISEE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()));

        Mockito.when(inpsInvocationServiceSpy.invoke(FISCAL_CODE, ISEE_TYPOLOGY)).thenReturn(Mono.just(Optional.of(inpsResponse)));
        Mockito.doAnswer(i -> {
            onboardingDTO.setIsee(PdndInvocationsTestUtils.getIseeFromResponse(inpsResponse));
            return null;
        }).when(inpsInvocationServiceSpy).extract(inpsResponse, true, onboardingDTO);

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new BigDecimal("10000"), result.getIsee());
        Assertions.assertNull(result.getResidence());
        Assertions.assertNull(result.getBirthDate());

        Mockito.verify(inpsInvocationServiceSpy).invoke(FISCAL_CODE, ISEE_TYPOLOGY);
        Mockito.verify(anprInvocationServiceSpy, Mockito.never()).invoke(ACCESS_TOKEN, FISCAL_CODE, agidTokenPayload);
    }

    @Test
    void testDailyLimitReached() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("ISEE", "RESIDENCE", "BIRTHDATE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()));

        Mockito.when(inpsInvocationServiceSpy.invoke(FISCAL_CODE, ISEE_TYPOLOGY)).thenReturn(Mono.just(Optional.empty()));
        Mockito.when(anprInvocationServiceSpy.invoke(ACCESS_TOKEN, FISCAL_CODE, agidTokenPayload)).thenReturn(Mono.just(Optional.empty()));

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        // Then
        Assertions.assertNull(result);

        Mockito.verify(inpsInvocationServiceSpy).invoke(FISCAL_CODE, ISEE_TYPOLOGY);
        Mockito.verify(anprInvocationServiceSpy).invoke(ACCESS_TOKEN, FISCAL_CODE, agidTokenPayload);
        Mockito.verify(onboardingRescheduleServiceMock)
                .reschedule(Mockito.eq(onboardingDTO), Mockito.argThat(schedule -> schedule.isAfter(TEST_DATE_TIME) && schedule.isBefore(OffsetDateTime.now().plusMinutes(60))), Mockito.eq("Daily limit reached"), Mockito.any());
    }
}