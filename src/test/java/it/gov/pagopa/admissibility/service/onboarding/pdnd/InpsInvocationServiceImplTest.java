package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.generated.soap.ws.client.ConsultazioneIndicatoreResponseType;
import it.gov.pagopa.admissibility.generated.soap.ws.client.EsitoEnum;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.soap.inps.IseeConsultationSoapClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class InpsInvocationServiceImplTest {

    private static final String FISCAL_CODE = "fiscalCode";
    public static final IseeTypologyEnum ISEE_TYPOLOGY = IseeTypologyEnum.UNIVERSITARIO;

    @Mock
    private IseeConsultationSoapClient iseeConsultationSoapClientMock;
    @Mock
    private CriteriaCodeService criteriaCodeServiceMock;

    private InpsInvocationService inpsInvocationService;

    private ConsultazioneIndicatoreResponseType inpsResponse;
    private OnboardingDTO onboardingRequest;

    @BeforeEach
    void setup() throws JAXBException {
        inpsInvocationService = new InpsInvocationServiceImpl(criteriaCodeServiceMock, iseeConsultationSoapClientMock);

        Mockito.lenient().when(criteriaCodeServiceMock.getCriteriaCodeConfig(Mockito.anyString())).thenReturn(new CriteriaCodeConfig(
                "ISEE",
                "INPS",
                "Istituto Nazionale Previdenza Sociale",
                "ISEE"
        ));

        inpsResponse = PdndInvocationsTestUtils.buildInpsResponse(EsitoEnum.OK);

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
        Optional<ConsultazioneIndicatoreResponseType> result = inpsInvocationService.invoke(FISCAL_CODE, ISEE_TYPOLOGY).block();

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
        Optional<ConsultazioneIndicatoreResponseType> result = inpsInvocationService.invoke(FISCAL_CODE, ISEE_TYPOLOGY).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void testExtract() {
        // When
        inpsInvocationService.extract(inpsResponse, true, onboardingRequest);

        // Then
        Assertions.assertEquals(BigDecimal.valueOf(10000), onboardingRequest.getIsee());
    }

    @Test
    void testExtractWhenResponseNull() {
        // When
        inpsInvocationService.extract(null, true, onboardingRequest);

        // Then
        Assertions.assertNull(onboardingRequest.getIsee());
    }
}