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
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
    public static final IseeTypologyEnum ISEE_TYPOLOGY = IseeTypologyEnum.UNIVERSITARIO;

    @Mock
    private IseeConsultationSoapClient iseeConsultationSoapClientMock;
    @Mock
    private CriteriaCodeService criteriaCodeServiceMock;

    private InpsDataRetrieverService inpsDataRetrieverService;

    private ConsultazioneIndicatoreResponseType inpsResponse;
    private BigDecimal expectedIsee;

    @BeforeEach
    void setup() throws JAXBException {
        CriteriaCodeConfigFaker.configCriteriaCodeServiceMock(criteriaCodeServiceMock);
        inpsDataRetrieverService = new InpsDataRetrieverServiceImpl(criteriaCodeServiceMock, iseeConsultationSoapClientMock);

        inpsResponse = InpsInvokeTestUtils.buildInpsResponse(EsitoEnum.OK);
        expectedIsee = BigDecimal.valueOf(10000);
    }

    private PdndServicesInvocation buildPdndServicesInvocation(boolean getIsee) {
        return new PdndServicesInvocation(getIsee, List.of(IseeTypologyEnum.UNIVERSITARIO), false, false);
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
        if(getIsee){
            Mockito.when(iseeConsultationSoapClientMock.getIsee(FISCAL_CODE, ISEE_TYPOLOGY)).thenReturn(Mono.just(inpsResponse));
        }

        // When
        Optional<List<OnboardingRejectionReason>> result = inpsDataRetrieverService.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, buildPdndServicesInvocation(getIsee), onboardingRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Collections.emptyList(), result.get());

        if(getIsee){
            TestUtils.assertBigDecimalEquals(expectedIsee, onboardingRequest.getIsee());
        } else {
            Assertions.assertNull(onboardingRequest.getIsee());
        }
    }

    @Test
    void testInvokeOk_koEsitoResult() {
        // Given
        inpsResponse.setEsito(EsitoEnum.RICHIESTA_INVALIDA);
        OnboardingDTO onboardingRequest = new OnboardingDTO();

        Mockito.when(iseeConsultationSoapClientMock.getIsee(FISCAL_CODE, ISEE_TYPOLOGY)).thenReturn(Mono.empty());

        // When
        Optional<List<OnboardingRejectionReason>> result = inpsDataRetrieverService.invoke(FISCAL_CODE, PDND_INITIATIVE_CONFIG, buildPdndServicesInvocation(true), onboardingRequest).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
        Assertions.assertNull(onboardingRequest.getIsee());
    }

    @Test
    void testExtractWhenResponseNull() throws JAXBException {
        inpsResponse=null;
        testExtractWhenNoIsee();

        inpsResponse=new ConsultazioneIndicatoreResponseType();
        testExtractWhenNoIsee();

        inpsResponse.setXmlEsitoIndicatore("".getBytes(StandardCharsets.UTF_8));
        testExtractWhenNoIsee();

        inpsResponse.setXmlEsitoIndicatore(InpsInvokeTestUtils.buildXmlResult(null));
        testExtractWhenNoIsee();
    }

    private void testExtractWhenNoIsee() {
        // Given
        Mockito.when(iseeConsultationSoapClientMock.getIsee(FISCAL_CODE, ISEE_TYPOLOGY)).thenReturn(Mono.just(inpsResponse));
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