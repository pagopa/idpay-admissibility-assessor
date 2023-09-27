package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.connector.soap.inps.IseeConsultationSoapClient;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.EsitoEnum;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.test.fakers.CriteriaCodeConfigFaker;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class InpsDataRetrieverServiceImplTest {

    private static final String FISCAL_CODE = "fiscalCode";
    public static final IseeTypologyEnum ISEE_TYPOLOGY = IseeTypologyEnum.UNIVERSITARIO;

    @Mock
    private IseeConsultationSoapClient iseeConsultationSoapClientMock;
    @Mock
    private CriteriaCodeService criteriaCodeServiceMock;

    private InpsDataRetrieverService inpsDataRetrieverService;

    private ConsultazioneIndicatoreResponseType inpsResponse;
    private BigDecimal expectedIsee;

    private OnboardingDTO onboardingRequest;

    @BeforeEach
    void setup() throws JAXBException {
        CriteriaCodeConfigFaker.configCriteriaCodeServiceMock(criteriaCodeServiceMock);
        inpsDataRetrieverService = new InpsDataRetrieverServiceImpl(criteriaCodeServiceMock, iseeConsultationSoapClientMock);

        inpsResponse = PdndInvocationsTestUtils.buildInpsResponse(EsitoEnum.OK);
        expectedIsee = BigDecimal.valueOf(10000);

        onboardingRequest = OnboardingDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVEID")
                .tc(true)
                .status("STATUS")
                .pdndAccept(true)
                .tcAcceptTimestamp(LocalDateTime.of(2022, 10, 2, 10, 0, 0))
                .criteriaConsensusTimestamp(LocalDateTime.of(2022, 10, 2, 10, 0, 0))
                .build();
    }

    @Test
    void testInvokeOk() {
        // Given
        Mockito.when(iseeConsultationSoapClientMock.getIsee(FISCAL_CODE, ISEE_TYPOLOGY)).thenReturn(Mono.just(inpsResponse));

        // When
        Optional<ConsultazioneIndicatoreResponseType> result = inpsDataRetrieverService.invoke(FISCAL_CODE, ISEE_TYPOLOGY).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(inpsResponse, result.get());
    }

    @Test
    void testInvokeKo() {
        // Given
        inpsResponse.setEsito(EsitoEnum.RICHIESTA_INVALIDA);

        Mockito.when(iseeConsultationSoapClientMock.getIsee(FISCAL_CODE, ISEE_TYPOLOGY)).thenReturn(Mono.empty());

        // When
        Optional<ConsultazioneIndicatoreResponseType> result = inpsDataRetrieverService.invoke(FISCAL_CODE, ISEE_TYPOLOGY).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void testExtract() {
        // When
        List<OnboardingRejectionReason> result = inpsDataRetrieverService.extract(inpsResponse, true, onboardingRequest);

        // Then
        Assertions.assertEquals(Collections.emptyList(), result);
        Assertions.assertEquals(expectedIsee, onboardingRequest.getIsee());
    }

    @Test
    void testExtractWhenResponseNull() throws JAXBException {
        inpsResponse=null;
        testExtractWhenNoIsee();

        inpsResponse=new ConsultazioneIndicatoreResponseType();
        testExtractWhenNoIsee();

        inpsResponse.setXmlEsitoIndicatore("".getBytes(StandardCharsets.UTF_8));
        testExtractWhenNoIsee();

        inpsResponse.setXmlEsitoIndicatore(PdndInvocationsTestUtils.buildXmlResult(null));
        testExtractWhenNoIsee();
    }

    private void testExtractWhenNoIsee() {
        // When
        List<OnboardingRejectionReason> result = inpsDataRetrieverService.extract(inpsResponse, true, onboardingRequest);

        // Then
        Assertions.assertEquals(List.of(buildExpectedIseeKoRejectionReason()), result);
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