package it.gov.pagopa.admissibility.dto.onboarding.mapper;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class Onboarding2EvaluationMapperTest {

    @Test
    void onboarding2EvaluationOnboardingOkTest() {

        // GIVEN
        Map<String, Boolean> selfDeclarationListMock1 = new HashMap<>();
        selfDeclarationListMock1.put("MAP", true);

        LocalDateTime localDateTimeMock1 = LocalDateTime.now();

        OnboardingDTO objectMock1 = new OnboardingDTO(
                "1",
                "1",
                true,
                "OK",
                true,
                selfDeclarationListMock1,
                localDateTimeMock1,
                localDateTimeMock1,
                new BigDecimal(100),
                null,
                LocalDate.of(2000,1,1)
        );

        List<String> rejectReasonsMock1 = new ArrayList<>();

        Onboarding2EvaluationMapper onboarding2EvaluationMapper = new Onboarding2EvaluationMapper();

        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(objectMock1, rejectReasonsMock1);

        // THEN
        Assertions.assertEquals("1", result.getUserId());
        Assertions.assertEquals("1", result.getInitiativeId());
        Assertions.assertEquals("ONBOARDING_OK", result.getStatus());
        Assertions.assertTrue(CollectionUtils.isEmpty(result.getOnboardingRejectionReasons()));
    }

    @Test
    void onboarding2EvaluationOnboardingKoTest() {

        // GIVEN
        Map<String, Boolean> selfDeclarationListMock1 = new HashMap<>();
        selfDeclarationListMock1.put("MAP", true);

        LocalDateTime localDateTimeMock1 = LocalDateTime.now();

        OnboardingDTO objectMock1 = new OnboardingDTO(
                "1",
                null,
                true,
                "OK",
                true,
                selfDeclarationListMock1,
                localDateTimeMock1,
                localDateTimeMock1,
                new BigDecimal(100),
                null,
                LocalDate.of(2000,1,1)
        );

        List<String> rejectReasonsMock1 = Collections.singletonList("InitiativeId NULL");

        Onboarding2EvaluationMapper onboarding2EvaluationMapper = new Onboarding2EvaluationMapper();

        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(objectMock1, rejectReasonsMock1);

        // THEN
        Assertions.assertEquals("1", result.getUserId());
        Assertions.assertNull(result.getInitiativeId());
        Assertions.assertEquals("ONBOARDING_KO", result.getStatus());
        Assertions.assertFalse(CollectionUtils.isEmpty(result.getOnboardingRejectionReasons()));

        //TODO check not null fields
    }
}
