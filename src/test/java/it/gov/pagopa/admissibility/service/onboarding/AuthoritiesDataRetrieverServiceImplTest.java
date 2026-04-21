package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.config.CriteriaCodeConfigs;
import it.gov.pagopa.admissibility.config.PagoPaAnprPdndConfig;
import it.gov.pagopa.admissibility.connector.rest.anpr.service.AnprDataRetrieverService;
import it.gov.pagopa.admissibility.connector.soap.inps.service.InpsDataRetrieverService;
import it.gov.pagopa.admissibility.connector.soap.inps.service.InpsThresholdRetrieverService;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.VerifyDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.exception.PdndException;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.onboarding.notifier.OnboardingRescheduleService;
import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthoritiesDataRetrieverServiceImplTest {

    private static final String USER_ID = "USERID";
    private static final String FISCAL_CODE = "FISCAL_CODE";

    private static final PdndInitiativeConfig PDND_CONFIG =
            new PdndInitiativeConfig("CLIENTID", "KID", "PURPOSEID");

    @Mock private UserFiscalCodeService userFiscalCodeServiceMock;
    @Mock private InpsDataRetrieverService inpsDataRetrieverServiceMock;
    @Mock private InpsThresholdRetrieverService inpsThresholdRetrieverServiceMock;
    @Mock private AnprDataRetrieverService anprDataRetrieverServiceMock;
    @Mock private OnboardingRescheduleService onboardingRescheduleServiceMock;
    @Mock private PagoPaAnprPdndConfig pagoPaAnprPdndConfigMock;
    @Mock private CriteriaCodeConfigs criteriaCodeConfigsMock;

    private AuthoritiesDataRetrieverService service;
    private OnboardingDTO onboardingRequest;
    private Message<String> message;

    /** Mappa mutabile per configurazioni criteri */
    private final Map<String, CriteriaCodeConfigs.CriteriaConfig> criteriaMap = new HashMap<>();

    @BeforeEach
    void setUp() {

        service = new AuthoritiesDataRetrieverServiceImpl(
                60L,
                false,
                onboardingRescheduleServiceMock,
                userFiscalCodeServiceMock,
                inpsDataRetrieverServiceMock,
                inpsThresholdRetrieverServiceMock,
                anprDataRetrieverServiceMock,
                pagoPaAnprPdndConfigMock,
                criteriaCodeConfigsMock
        );

        onboardingRequest = OnboardingDTO.builder()
                .userId(USER_ID)
                .initiativeId("INITIATIVEID")
                .verifies(new ArrayList<>())
                .build();

        message = MessageBuilder.withPayload("{}").build();

        criteriaMap.clear();
    }

    @Test
    void retrieveAllAuthorities_ok() {

        stubCommonPdnd();

        addVerify("ISEE", true, null);
        addVerify("RESIDENCE", true, null);
        addVerify("BIRTHDATE", true, null);

        mockCriteria("ISEE", "INPS", "c001");
        mockCriteria("RESIDENCE", "AGID", "c001");
        mockCriteria("BIRTHDATE", "ANPR", "c001");

        BigDecimal expectedIsee = BigDecimal.TEN;
        Residence expectedResidence =
                Residence.builder().city("Milano").province("MI").postalCode("20143").build();
        BirthDate expectedBirthDate =
                BirthDate.builder().year("2001").age(LocalDate.now().getYear() - 2001).build();

        when(inpsDataRetrieverServiceMock.invoke(any(), any(), any(), any()))
                .thenAnswer(a -> {
                    onboardingRequest.setIsee(expectedIsee);
                    return Mono.just(Optional.of(List.of()));
                });

        when(anprDataRetrieverServiceMock.invoke(any(), any(), any(), any()))
                .thenAnswer(a -> {
                    onboardingRequest.setResidence(expectedResidence);
                    onboardingRequest.setBirthDate(expectedBirthDate);
                    return Mono.just(Optional.of(List.of()));
                });

        OnboardingDTO result = service.retrieve(onboardingRequest, null, message).block();

        assertNotNull(result);
        assertEquals(expectedIsee, result.getIsee());
        assertEquals(expectedResidence, result.getResidence());
        assertEquals(expectedBirthDate, result.getBirthDate());

        assertTrue(findVerify("ISEE").getResult());
        assertTrue(findVerify("RESIDENCE").getResult());
        assertTrue(findVerify("BIRTHDATE").getResult());
    }

    @Test
    void retrieveAuthority_verifyKo_setsResultFalse() {

        stubCommonPdnd();

        addVerify("ISEE", true, null);
        mockCriteria("ISEE", "INPS", "c001");

        when(inpsDataRetrieverServiceMock.invoke(any(), any(), any(), any()))
                .thenReturn(Mono.just(Optional.of(
                        List.of(new OnboardingRejectionReason(
                                OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                                "ISEE", null, null, null))
                )));

        service.retrieve(onboardingRequest, null, message).block();

        assertFalse(findVerify("ISEE").getResult());
    }

    @Test
    void retrieveAuthority_verifyDisabled_skippedAndMarkedOk() {

        when(userFiscalCodeServiceMock.getUserFiscalCode(USER_ID))
                .thenReturn(Mono.just(FISCAL_CODE));

        addVerify("ISEE", false, null);

        OnboardingDTO result =
                service.retrieve(onboardingRequest, null, message).block();

        assertNotNull(result);
        assertTrue(findVerify("ISEE").getResult());

        verifyNoInteractions(inpsDataRetrieverServiceMock);
        verifyNoInteractions(anprDataRetrieverServiceMock);
    }

    @Test
    void retrieveAuthority_pdndDown_rescheduledAndExceptionThrown() {

        stubCommonPdnd();

        addVerify("ISEE", true, null);
        mockCriteria("ISEE", "INPS", "c001");

        when(inpsDataRetrieverServiceMock.invoke(any(), any(), any(), any()))
                .thenReturn(Mono.just(Optional.empty()));

        assertThrows(
                PdndException.class,
                () -> service.retrieve(onboardingRequest, null, message).block()
        );

        verify(onboardingRescheduleServiceMock)
                .reschedule(eq(onboardingRequest), any(), anyString(), eq(message));
    }

    @Test
    void retrieveAuthorities_allResultsAlreadyPresent_shortCircuit() {

        addVerify("ISEE", true, null);
        findVerify("ISEE").setResult(true);

        service.retrieve(onboardingRequest, null, message).block();

        verifyNoInteractions(userFiscalCodeServiceMock);
        verifyNoInteractions(inpsDataRetrieverServiceMock);
        verifyNoInteractions(anprDataRetrieverServiceMock);
    }

    @Test
    void retrieveAuthority_inpsWithThreshold_usesThresholdService() {

        stubCommonPdnd();

        addVerify("ISEE", true, "THRESHOLD_1");
        mockCriteria("ISEE", "INPS", "c001");

        when(inpsThresholdRetrieverServiceMock.invoke(any(), any(), any(), any()))
                .thenReturn(Mono.just(Optional.of(List.of())));

        service.retrieve(onboardingRequest, null, message).block();

        assertTrue(findVerify("ISEE").getResult());

        verify(inpsThresholdRetrieverServiceMock).invoke(any(), any(), any(), any());
        verifyNoInteractions(inpsDataRetrieverServiceMock);
        verifyNoInteractions(anprDataRetrieverServiceMock);
    }

    @Test
    void retrieveAuthority_agid_usesAnprService() {

        stubCommonPdnd();

        addVerify("RESIDENCE", true, null);
        mockCriteria("RESIDENCE", "AGID", "c001");

        Residence expectedResidence =
                Residence.builder().city("Roma").province("RM").postalCode("00100").build();

        when(anprDataRetrieverServiceMock.invoke(any(), any(), any(), any()))
                .thenAnswer(a -> {
                    onboardingRequest.setResidence(expectedResidence);
                    return Mono.just(Optional.of(List.of()));
                });

        service.retrieve(onboardingRequest, null, message).block();

        assertEquals(expectedResidence, onboardingRequest.getResidence());
        assertTrue(findVerify("RESIDENCE").getResult());

        verify(anprDataRetrieverServiceMock).invoke(any(), any(), any(), any());
        verifyNoInteractions(inpsDataRetrieverServiceMock);
        verifyNoInteractions(inpsThresholdRetrieverServiceMock);
    }

    @Test
    void retrieveAuthority_anpr_usesAnprService() {

        stubCommonPdnd();

        addVerify("BIRTHDATE", true, null);
        mockCriteria("BIRTHDATE", "ANPR", "c001");

        BirthDate expectedBirthDate =
                BirthDate.builder().year("1995").age(LocalDate.now().getYear() - 1995).build();

        when(anprDataRetrieverServiceMock.invoke(any(), any(), any(), any()))
                .thenAnswer(a -> {
                    onboardingRequest.setBirthDate(expectedBirthDate);
                    return Mono.just(Optional.of(List.of()));
                });

        service.retrieve(onboardingRequest, null, message).block();

        assertEquals(expectedBirthDate, onboardingRequest.getBirthDate());
        assertTrue(findVerify("BIRTHDATE").getResult());

        verify(anprDataRetrieverServiceMock).invoke(any(), any(), any(), any());
        verifyNoInteractions(inpsDataRetrieverServiceMock);
        verifyNoInteractions(inpsThresholdRetrieverServiceMock);
    }

    private void stubCommonPdnd() {
        when(userFiscalCodeServiceMock.getUserFiscalCode(USER_ID))
                .thenReturn(Mono.just(FISCAL_CODE));

        when(pagoPaAnprPdndConfigMock.getPagopaPdndConfiguration())
                .thenReturn(Map.of("c001", PDND_CONFIG));

        when(criteriaCodeConfigsMock.getConfigs())
                .thenReturn(criteriaMap);
    }

    private void mockCriteria(String code, String authority, String pdndClient) {
        CriteriaCodeConfigs.CriteriaConfig cfg = new CriteriaCodeConfigs.CriteriaConfig();
        cfg.setAuthority(authority);
        cfg.setPdndClient(pdndClient);
        criteriaMap.put(code, cfg);
    }

    private void addVerify(String code, boolean verify, String thresholdCode) {
        onboardingRequest.getVerifies().add(
                VerifyDTO.builder()
                        .code(code)
                        .verify(verify)
                        .thresholdCode(thresholdCode)
                        .build()
        );
    }

    private VerifyDTO findVerify(String code) {
        return onboardingRequest.getVerifies().stream()
                .filter(v -> v.getCode().equals(code))
                .findFirst()
                .orElseThrow();
    }
}