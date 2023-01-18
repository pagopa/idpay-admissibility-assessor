package it.gov.pagopa.admissibility.service.onboarding.pdnd;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.RispostaE002OKDTO;
import it.gov.pagopa.admissibility.mapper.TipoResidenzaDTO2ResidenceMapper;
import it.gov.pagopa.admissibility.rest.anpr.exception.AnprDailyRequestLimitException;
import it.gov.pagopa.admissibility.service.pdnd.residence.ResidenceAssessmentService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AnprInvocationServiceImplTest {
    private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    private static final String FISCAL_CODE = "fiscalCode";

    @Mock
    private ResidenceAssessmentService residenceAssessmentServiceMock;

    private final TipoResidenzaDTO2ResidenceMapper residenceMapper = new TipoResidenzaDTO2ResidenceMapper();

    private RispostaE002OKDTO anprAnswer;
    private OnboardingDTO onboardingRequest;

    private AnprInvocationService anprInvocationService;

    @BeforeEach
    void setup() {
        anprInvocationService = new AnprInvocationServiceImpl(residenceAssessmentServiceMock, residenceMapper);

        anprAnswer = PdndInvocationsTestUtils.buildAnprResponse();

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
    void testInvokeOK() {
        // Given
        Mockito.when(residenceAssessmentServiceMock.getResidenceAssessment(ACCESS_TOKEN, FISCAL_CODE)).thenReturn(Mono.just(anprAnswer));

        // When
        Optional<RispostaE002OKDTO> result = anprInvocationService.invoke(ACCESS_TOKEN, FISCAL_CODE).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(anprAnswer, result.get());
    }

    @Test
    void testInvokeDailyLimitException() {
        // Given
        Mockito.when(residenceAssessmentServiceMock.getResidenceAssessment(ACCESS_TOKEN, FISCAL_CODE)).thenReturn(Mono.error(AnprDailyRequestLimitException::new));

        // When
        Optional<RispostaE002OKDTO> result = anprInvocationService.invoke(ACCESS_TOKEN, FISCAL_CODE).block();

        // Then
        Assertions.assertEquals(Optional.empty(), result);
    }

    @Test
    void testExtract() {
        // Given
        Residence expectedResidence = Residence.builder().city("Milano").province("MI").postalCode("20143").build();
        BirthDate expectedBirthDate = BirthDate.builder().year("2001").age(21).build();

        // When
        anprInvocationService.extract(anprAnswer, true, true, onboardingRequest);

        // Then
        Assertions.assertEquals(expectedResidence, onboardingRequest.getResidence());
        Assertions.assertEquals(expectedBirthDate, onboardingRequest.getBirthDate());
    }

    @Test
    void testExtractWhenResponseNull() {
        // When
        anprInvocationService.extract(null, true, true, onboardingRequest);

        // Then
        Assertions.assertNull(onboardingRequest.getResidence());
        Assertions.assertNull(onboardingRequest.getBirthDate());
    }
}