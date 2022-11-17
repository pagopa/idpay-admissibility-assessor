package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.onboarding.EvaluationDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.TestUtils;
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

    private final Onboarding2EvaluationMapper onboarding2EvaluationMapper = new Onboarding2EvaluationMapper();

    @Test
    void onboarding2EvaluationOnboardingOkTest() {

        // GIVEN
        Map<String, Boolean> selfDeclarationList = new HashMap<>();
        selfDeclarationList.put("MAP", true);

        LocalDateTime acceptanceDateTime = LocalDateTime.now();

        OnboardingDTO onboardingRequest = new OnboardingDTO(
                "USERID",
                "INITIATIVEID",
                true,
                "OK",
                true,
                selfDeclarationList,
                acceptanceDateTime,
                acceptanceDateTime,
                new BigDecimal(100),
                new Residence(),
                new BirthDate()
        );

        List<OnboardingRejectionReason> rejectReasons = new ArrayList<>();

        InitiativeConfig initiativeConfig = new InitiativeConfig();
        initiativeConfig.setInitiativeId("INITIATIVEID");
        initiativeConfig.setInitiativeName("INITIATIVENAME");
        initiativeConfig.setOrganizationId("ORGANIZATIONID");
        initiativeConfig.setBeneficiaryInitiativeBudget(BigDecimal.TEN);
        initiativeConfig.setServiceId("SERVICEID");

        LocalDate endDate = LocalDate.now();
        initiativeConfig.setEndDate(endDate);


        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(onboardingRequest, initiativeConfig, rejectReasons);

        // THEN
        Assertions.assertEquals("USERID", result.getUserId());
        Assertions.assertEquals("INITIATIVEID", result.getInitiativeId());
        Assertions.assertEquals("INITIATIVENAME", result.getInitiativeName());
        Assertions.assertEquals("ORGANIZATIONID", result.getOrganizationId());
        Assertions.assertEquals("ONBOARDING_OK", result.getStatus());
        Assertions.assertEquals("SERVICEID", result.getServiceId());
        Assertions.assertEquals(endDate, result.getInitiativeEndDate());
        Assertions.assertEquals(0, BigDecimal.TEN.compareTo(result.getBeneficiaryBudget()));
        Assertions.assertTrue(CollectionUtils.isEmpty(result.getOnboardingRejectionReasons()));
        Assertions.assertEquals(acceptanceDateTime, result.getCriteriaConsensusTimestamp());

        TestUtils.checkNotNullFields(result);
    }

    @Test
    void onboarding2EvaluationOnboardingKoTest() {

        // GIVEN
        Map<String, Boolean> selfDeclarationListMock1 = new HashMap<>();
        selfDeclarationListMock1.put("MAP", true);

        LocalDateTime localDateTimeMock1 = LocalDateTime.now();

        OnboardingDTO objectMock1 = new OnboardingDTO(
                "1",
                "ID",
                true,
                "OK",
                true,
                selfDeclarationListMock1,
                localDateTimeMock1,
                localDateTimeMock1,
                new BigDecimal(100),
                new Residence(),
                new BirthDate()
        );

        List<OnboardingRejectionReason> rejectReasons = Collections.singletonList(OnboardingRejectionReason.builder()
                .type(OnboardingRejectionReason.OnboardingRejectionReasonType.INVALID_REQUEST)
                .code("InitiativeId NULL")
                .build());

        // WHEN
        EvaluationDTO result = onboarding2EvaluationMapper.apply(objectMock1, null, rejectReasons);

        // THEN
        Assertions.assertEquals("1", result.getUserId());
        Assertions.assertEquals("ID", result.getInitiativeId());
        Assertions.assertEquals("ONBOARDING_KO", result.getStatus());

        Assertions.assertNull(result.getInitiativeName());
        Assertions.assertNull(result.getOrganizationId());
        Assertions.assertNull(result.getBeneficiaryBudget());
        Assertions.assertNull(result.getInitiativeEndDate());
        Assertions.assertNull(result.getServiceId());

        Assertions.assertEquals(rejectReasons, result.getOnboardingRejectionReasons());
        Assertions.assertEquals(localDateTimeMock1, result.getCriteriaConsensusTimestamp());

        TestUtils.checkNotNullFields(result, "initiativeName", "organizationId", "serviceId", "initiativeEndDate", "beneficiaryBudget");
    }
}
