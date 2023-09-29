package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprDataRetrieverService;
import it.gov.pagopa.admissibility.connector.soap.inps.service.InpsDataRetrieverService;
import it.gov.pagopa.admissibility.connector.soap.inps.utils.InpsInvokeTestUtils;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.exception.OnboardingException;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.EsitoEnum;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.model.Order;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.notifier.OnboardingRescheduleService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static it.gov.pagopa.admissibility.utils.OnboardingConstants.*;

@ExtendWith(MockitoExtension.class)
class AuthoritiesDataRetrieverServiceImplTest {

    private static final String FISCAL_CODE = "FISCAL_CODE";
    public static final PdndInitiativeConfig PDND_INITIATIVE_CONFIG = new PdndInitiativeConfig(
            "CLIENTID",
            "KID",
            "PURPOSEID"
    );
    public static final List<IseeTypologyEnum> ISEE_TYPOLOGIES_REQUESTED = List.of(IseeTypologyEnum.UNIVERSITARIO, IseeTypologyEnum.ORDINARIO);

    private final OffsetDateTime TEST_DATE_TIME = OffsetDateTime.now();

    @Mock
    private UserFiscalCodeService userFiscalCodeServiceMock;
    @Mock
    private AnprDataRetrieverService anprDataRetrieverServiceSpy;
    @Mock
    private InpsDataRetrieverService inpsDataRetrieverServiceSpy;
    @Mock
    private OnboardingRescheduleService onboardingRescheduleServiceMock;

    private AuthoritiesDataRetrieverService authoritiesDataRetrieverService;

    private OnboardingDTO onboardingRequest;
    private InitiativeConfig initiativeConfig;
    private Message<String> message;

    private BigDecimal expectedIsee;
    private Residence expectedResidence;
    private BirthDate expectedBirthDate;

    @BeforeEach
    void setUp() {
        PagoPaAnprPdndConfig pagoPaAnprPdndConfig = new PagoPaAnprPdndConfig();
        pagoPaAnprPdndConfig.setClientId("CLIENTID");
        pagoPaAnprPdndConfig.setKid("KID");
        pagoPaAnprPdndConfig.setPurposeId("PURPOSEID");

        authoritiesDataRetrieverService = new AuthoritiesDataRetrieverServiceImpl(60L, false, onboardingRescheduleServiceMock, userFiscalCodeServiceMock, inpsDataRetrieverServiceSpy, anprDataRetrieverServiceSpy, pagoPaAnprPdndConfig);

        onboardingRequest = OnboardingDTO.builder()
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
                .initiativeBudget(new BigDecimal("100"))
                .beneficiaryInitiativeBudget(BigDecimal.TEN)
                .rankingInitiative(Boolean.TRUE)
                .automatedCriteria(List.of(
                        new AutomatedCriteriaDTO("AUTH1", CRITERIA_CODE_ISEE, null, FilterOperator.EQ, "1", null, Sort.Direction.ASC, ISEE_TYPOLOGIES_REQUESTED)))
                .build();

        ConsultazioneIndicatoreResponseType inpsResponse = InpsInvokeTestUtils.buildInpsResponse(EsitoEnum.OK);
        expectedIsee = InpsInvokeTestUtils.getIseeFromResponse(inpsResponse);

        expectedResidence = Residence.builder().city("Milano").province("MI").postalCode("20143").build();
        expectedBirthDate = BirthDate.builder().year("2001").age(LocalDate.now().getYear() - 2001).build();

        message = MessageBuilder.withPayload(TestUtils.jsonSerializer(onboardingRequest)).build();
    }

    @AfterEach
    void verifyMockNoMoreInvocation() {
        Mockito.verifyNoMoreInteractions(userFiscalCodeServiceMock, inpsDataRetrieverServiceSpy, anprDataRetrieverServiceSpy, onboardingRescheduleServiceMock);
    }

    //region test utilities
    private PdndServicesInvocation configureAuthoritiesDataRetrieverMocks(boolean getIsee, boolean getResidence, boolean getBirthDate) {
        PdndServicesInvocation expectedPdndServicesInvocation = buildExpectedPdndServiceInvocation(getIsee, getResidence, getBirthDate);
        if (expectedPdndServicesInvocation.requirePdndInvocation()) {
            Mockito.when(userFiscalCodeServiceMock.getUserFiscalCode(onboardingRequest.getUserId())).thenReturn(Mono.just(FISCAL_CODE));
        }
        configureInpsDataRetriever(expectedPdndServicesInvocation);
        configureAnprDataRetriever(expectedPdndServicesInvocation);
        return expectedPdndServicesInvocation;
    }

    private PdndServicesInvocation buildExpectedPdndServiceInvocation(boolean getIsee, boolean getResidence, boolean getBirthDate) {
        return new PdndServicesInvocation(getIsee, ISEE_TYPOLOGIES_REQUESTED, getResidence, getBirthDate);
    }

    private void configureAnprDataRetriever(PdndServicesInvocation expectedPdndServicesInvocation) {
        Mockito.when(anprDataRetrieverServiceSpy.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, expectedPdndServicesInvocation, onboardingRequest)).thenReturn(Mono.defer(() -> {
            if (expectedPdndServicesInvocation.isGetResidence()) {
                onboardingRequest.setResidence(expectedResidence);
            }
            if (expectedPdndServicesInvocation.isGetBirthDate()) {
                onboardingRequest.setBirthDate(expectedBirthDate);
            }
            return Mono.just(Optional.of(Collections.emptyList()));
        }));
    }

    private void configureInpsDataRetriever(PdndServicesInvocation expectedPdndServicesInvocation) {
        Mockito.when(inpsDataRetrieverServiceSpy.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, expectedPdndServicesInvocation, onboardingRequest)).thenReturn(Mono.defer(() -> {
            if (expectedPdndServicesInvocation.isGetIsee()) {
                onboardingRequest.setIsee(expectedIsee);
            }
            return Mono.just(Optional.of(Collections.emptyList()));
        }));
    }
//endregion

    @ParameterizedTest
    @CsvSource({
            "true, true, true",
            "true, false, false",
            "false, true, false",
            "false, false, true",
    })
    void retrieveAllAuthorities_AutomatedCriteria(boolean getIsee, boolean getResidence, boolean getBirthDate) {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(
                Stream.of(getIsee ? CRITERIA_CODE_ISEE : null,
                                getResidence ? CRITERIA_CODE_RESIDENCE : null,
                                getBirthDate ? CRITERIA_CODE_BIRTHDATE : null)
                        .filter(Objects::nonNull)
                        .toList());
        initiativeConfig.setRankingFields(Collections.emptyList());

        retrieveAllAuthorities(getIsee, getResidence, getBirthDate);
    }

    @ParameterizedTest
    @CsvSource({
            "true, true, true",
            "true, false, false",
            "false, true, false",
            "false, false, true",
    })
    void retrieveAllAuthorities_Ranking(boolean getIsee, boolean getResidence, boolean getBirthDate) {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(Collections.emptyList());
        initiativeConfig.setRankingFields(Stream.of(
                getIsee? Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build(): null,
                getResidence? Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build(): null,
                getBirthDate? Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_BIRTHDATE).direction(Sort.Direction.ASC).build(): null)
                .filter(Objects::nonNull)
                .toList());

        retrieveAllAuthorities(getIsee, getResidence, getBirthDate);
    }

    void retrieveAllAuthorities(boolean getIsee, boolean getResidence, boolean getBirthDate) {
        configureAuthoritiesDataRetrieverMocks(getIsee, getResidence, getBirthDate);

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingRequest, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(getIsee ? expectedIsee : null, result.getIsee());
        Assertions.assertEquals(getResidence ? expectedResidence : null, result.getResidence());
        Assertions.assertEquals(getBirthDate ? expectedBirthDate : null, result.getBirthDate());
    }

    @Test
    void dontRetrieveAuthorities_notRequired() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(Collections.emptyList());
        initiativeConfig.setRankingFields(Collections.emptyList());

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingRequest, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getIsee());
        Assertions.assertNull(result.getResidence());
        Assertions.assertNull(result.getBirthDate());
    }

    @Test
    void dontRetrieveAuthorities_alreadyProvided() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of(CRITERIA_CODE_ISEE, CRITERIA_CODE_RESIDENCE, CRITERIA_CODE_BIRTHDATE));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build(),
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build(),
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_BIRTHDATE).direction(Sort.Direction.ASC).build()));

        onboardingRequest.setIsee(expectedIsee);
        onboardingRequest.setResidence(expectedResidence);
        onboardingRequest.setBirthDate(expectedBirthDate);

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingRequest, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedIsee, result.getIsee());
        Assertions.assertEquals(expectedResidence, result.getResidence());
        Assertions.assertEquals(expectedBirthDate, result.getBirthDate());
    }


    @Test
    void testDailyLimitReached() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of(CRITERIA_CODE_ISEE, CRITERIA_CODE_RESIDENCE, CRITERIA_CODE_BIRTHDATE));
        initiativeConfig.setRankingFields(Collections.emptyList());

        PdndServicesInvocation expectedPdndServicesInvocation = configureAuthoritiesDataRetrieverMocks(true, true, true);
        Mockito.when(inpsDataRetrieverServiceSpy.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, expectedPdndServicesInvocation, onboardingRequest)).thenReturn(Mono.just(Optional.empty()));
        Mockito.when(anprDataRetrieverServiceSpy.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, expectedPdndServicesInvocation, onboardingRequest)).thenReturn(Mono.just(Optional.empty()));

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingRequest, initiativeConfig, message).block();

        // Then
        Assertions.assertNull(result);

        Mockito.verify(onboardingRescheduleServiceMock)
                .reschedule(Mockito.eq(onboardingRequest), Mockito.argThat(schedule -> schedule.isAfter(TEST_DATE_TIME) && schedule.isBefore(OffsetDateTime.now().plusMinutes(60))), Mockito.eq("Daily limit reached"), Mockito.any());
    }

    @Test
    void testRejectionReasons() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of(CRITERIA_CODE_ISEE, CRITERIA_CODE_RESIDENCE, CRITERIA_CODE_BIRTHDATE));

        OnboardingRejectionReason expectedRejectionReasonISEE = new OnboardingRejectionReason(OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO, CRITERIA_CODE_ISEE, null, null, null);
        OnboardingRejectionReason expectedRejectionReasonResidence = new OnboardingRejectionReason(OnboardingRejectionReason.OnboardingRejectionReasonType.RESIDENCE_KO, CRITERIA_CODE_RESIDENCE, null, null, null);
        OnboardingRejectionReason expectedRejectionReasonBirthdate = new OnboardingRejectionReason(OnboardingRejectionReason.OnboardingRejectionReasonType.BIRTHDATE_KO, CRITERIA_CODE_BIRTHDATE, null, null, null);

        PdndServicesInvocation expectedPdndServicesInvocation = configureAuthoritiesDataRetrieverMocks(true, true, true);

        Mockito.when(inpsDataRetrieverServiceSpy.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, expectedPdndServicesInvocation, onboardingRequest))
                .thenReturn(Mono.just(Optional.of(List.of(expectedRejectionReasonISEE))));

        Mockito.when(anprDataRetrieverServiceSpy.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, expectedPdndServicesInvocation, onboardingRequest))
                .thenReturn(Mono.just(Optional.of(List.of(expectedRejectionReasonResidence, expectedRejectionReasonBirthdate))));

        // When
        Mono<OnboardingDTO> retrieveMono = authoritiesDataRetrieverService.retrieve(onboardingRequest, initiativeConfig, message);
        try {
            retrieveMono.block();
            Assertions.fail("Expected exception");
        } catch (OnboardingException e) {
            Assertions.assertEquals(List.of(
                    expectedRejectionReasonISEE,
                    expectedRejectionReasonResidence,
                    expectedRejectionReasonBirthdate
            ), e.getRejectionReasons());
        }
    }
}