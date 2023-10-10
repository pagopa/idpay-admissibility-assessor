package it.gov.pagopa.admissibility.connector.soap.inps.service;

import it.gov.pagopa.admissibility.connector.pdnd.PdndServicesInvocation;
import it.gov.pagopa.admissibility.connector.soap.inps.utils.InpsInvokeTestUtils;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.EsitoEnum;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class InpsDataRetrieverServiceImplTest {

    public static final String FISCAL_CODE = "fiscalCode";
    public static final PdndInitiativeConfig PDND_INITIATIVE_CONFIG = new PdndInitiativeConfig(
            "CLIENTID",
            "KID",
            "PURPOSEID"
    );
    public static final IseeTypologyEnum ISEE_TYPOLOGY1 = IseeTypologyEnum.UNIVERSITARIO;
    public static final IseeTypologyEnum ISEE_TYPOLOGY2 = IseeTypologyEnum.CORRENTE;

    @Mock
    private IseeConsultationSoapClient iseeConsultationSoapClientMock;
    @Mock
    private CriteriaCodeService criteriaCodeServiceMock;

    private InpsDataRetrieverService inpsDataRetrieverService;

    private ConsultazioneIndicatoreResponseType inpsResponse;
    private BigDecimal expectedIsee;

    @BeforeEach
    void setup() {
        CriteriaCodeConfigFaker.configCriteriaCodeServiceMock(criteriaCodeServiceMock);
        inpsDataRetrieverService = new InpsDataRetrieverServiceImpl(criteriaCodeServiceMock, iseeConsultationSoapClientMock);

        inpsResponse = InpsInvokeTestUtils.buildInpsResponse(EsitoEnum.OK);
        expectedIsee = BigDecimal.valueOf(10000);
    }

    private PdndServicesInvocation buildPdndServicesInvocation(boolean getIsee) {
        return new PdndServicesInvocation(getIsee, List.of(ISEE_TYPOLOGY1, ISEE_TYPOLOGY2), false, false);
    }

    @Test
    void testInvokeOk_requireData() {
        testInvokeOk(true);
    }

    @Test
    void testInvokeOk_NorequireData() {
        testInvokeOk(false);
    }

    private void testInvokeOk(boolean getIsee) {
        // Given
        OnboardingDTO onboardingRequest = new OnboardingDTO();
        if (getIsee) {
            Mockito.when(iseeConsultationSoapClientMock.getIsee(FISCAL_CODE, ISEE_TYPOLOGY1)).thenReturn(Mono.just(inpsResponse));
        }

        // When
        Optional<List<OnboardingRejectionReason>> result = inpsDataRetrieverService.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, buildPdndServicesInvocation(getIsee), onboardingRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Collections.emptyList(), result.get());

        if (getIsee) {
            TestUtils.assertBigDecimalEquals(expectedIsee, onboardingRequest.getIsee());
        } else {
            Assertions.assertNull(onboardingRequest.getIsee());
        }
    }

    @Test
    void testInvokeOk_RetryEsitoResult() {
        // Given
        inpsResponse.setEsito(EsitoEnum.DATABASE_OFFLINE);
        OnboardingDTO onboardingRequest = new OnboardingDTO();

        Mockito.when(iseeConsultationSoapClientMock.getIsee(FISCAL_CODE, ISEE_TYPOLOGY1)).thenReturn(Mono.empty());

        // When
        Optional<List<OnboardingRejectionReason>> result = inpsDataRetrieverService.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, buildPdndServicesInvocation(true), onboardingRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(IseeTypologyEnum.class)
    void testInvokeOk_MultipleIsee(IseeTypologyEnum iseeTypeOk) {
        // Given
        OnboardingDTO onboardingRequest = new OnboardingDTO();

        List<IseeTypologyEnum> iseeTypes = Arrays.asList(IseeTypologyEnum.values());

        ConsultazioneIndicatoreResponseType koResult = new ConsultazioneIndicatoreResponseType();
        koResult.setEsito(EsitoEnum.RICHIESTA_INVALIDA);
        koResult.setXmlEsitoIndicatore(null);

        for (IseeTypologyEnum i : iseeTypes) {
            OngoingStubbing<Mono<ConsultazioneIndicatoreResponseType>> stub = Mockito.when(iseeConsultationSoapClientMock.getIsee(FISCAL_CODE, i));
            if (!i.equals(iseeTypeOk)) {
                stub.thenReturn(Mono.just(koResult));
            } else {
                stub.thenReturn(Mono.just(inpsResponse));
                break; // next typologies will not be invoked, thus we will not configure the stub
            }
        }

        PdndServicesInvocation pdndServicesInvocation = new PdndServicesInvocation(true, iseeTypes, false, false);

        // When
        Optional<List<OnboardingRejectionReason>> result = inpsDataRetrieverService.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, pdndServicesInvocation, onboardingRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Collections.emptyList(), result.get());

        TestUtils.assertBigDecimalEquals(expectedIsee, onboardingRequest.getIsee());
    }

    @Test
    void testInvokeOk_KoEsitoResult() {
        inpsResponse.setEsito(EsitoEnum.RICHIESTA_INVALIDA);
        inpsResponse.setXmlEsitoIndicatore(null);
        testExtractWhenNoIsee();

        inpsResponse.setXmlEsitoIndicatore("".getBytes(StandardCharsets.UTF_8));
        testExtractWhenNoIsee();

        inpsResponse.setXmlEsitoIndicatore(InpsInvokeTestUtils.buildXmlResult(null));
        testExtractWhenNoIsee();
    }

    private void testExtractWhenNoIsee() {
        // Given
        Mockito.when(iseeConsultationSoapClientMock.getIsee(FISCAL_CODE, ISEE_TYPOLOGY1)).thenReturn(Mono.just(inpsResponse));
        Mockito.when(iseeConsultationSoapClientMock.getIsee(FISCAL_CODE, ISEE_TYPOLOGY2)).thenReturn(Mono.just(inpsResponse));
        OnboardingDTO onboardingRequest = new OnboardingDTO();

        // When
        Optional<List<OnboardingRejectionReason>> result = inpsDataRetrieverService.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, buildPdndServicesInvocation(true), onboardingRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(List.of(buildExpectedIseeKoRejectionReason()), result.get());
        Assertions.assertNull(onboardingRequest.getIsee());
    }

    private OnboardingRejectionReason buildExpectedIseeKoRejectionReason() {
        return new OnboardingRejectionReason(
                OnboardingRejectionReason.OnboardingRejectionReasonType.ISEE_TYPE_KO,
                OnboardingConstants.REJECTION_REASON_ISEE_TYPE_KO,
                CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE_AUTH,
                CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE_AUTH_LABEL,
                "ISEE non disponibile"
        );
    }
}