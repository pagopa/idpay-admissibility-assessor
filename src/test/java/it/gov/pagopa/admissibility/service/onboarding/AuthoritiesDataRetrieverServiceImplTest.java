package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.Order;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

class AuthoritiesDataRetrieverServiceImplTest {

    @Mock
    private OnboardingContextHolderService onboardingContextHolderServiceMock;

    private AuthoritiesDataRetrieverService authoritiesDataRetrieverService;

    private OnboardingDTO onboardingDTO;
    private InitiativeConfig initiativeConfig;

    @BeforeEach
    void setUp() {
        authoritiesDataRetrieverService = new AuthoritiesDataRetrieverServiceImpl(onboardingContextHolderServiceMock, null, 60L, false);

        onboardingDTO =OnboardingDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVEID")
                .tc(true)
                .status("STATUS")
                .pdndAccept(true)
                .tcAcceptTimestamp(LocalDateTime.of(2022,10,2,10,0,0))
                .criteriaConsensusTimestamp(LocalDateTime.of(2022,10,2,10,0,0))
                .build();

        LocalDate now = LocalDate.now();
        initiativeConfig = InitiativeConfig.builder()
                .initiativeId("INITIATIVEID")
                .initiativeName("INITITIATIVE_NAME")
                .organizationId("ORGANIZATIONID")
                .status("STATUS")
                .startDate(now)
                .endDate(now)
                .pdndToken("PDND_TOKEN")
                .initiativeBudget(new BigDecimal("100"))
                .beneficiaryInitiativeBudget(BigDecimal.TEN)
                .rankingInitiative(Boolean.TRUE)
                .build();
        Message<String> message = MessageBuilder.withPayload(TestUtils.jsonSerializer(onboardingDTO)).build();
    }

    @Test
    void retrieveIseeAutomatedCriteriaAndRanking() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("ISEE", "RESIDENCE", "BIRTHDATE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()));

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new BigDecimal("74585"), result.getIsee());
    }

    @Test
    void retrieveIseeAutomatedCriteriaAndNotRanking() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("ISEE", "RESIDENCE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build()));

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new BigDecimal("74585"), result.getIsee());
    }

    @Test
    void retrieveIseeNotAutomatedCriteriaNotRanking() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("RESIDENCE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build()));

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getIsee());
    }

    @Test
    void retrieveIseeRankingAndNotAutomatedCriteria() {
        // Given
        initiativeConfig.setAutomatedCriteriaCodes(List.of("RESIDENCE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build(),
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()

        ));

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new BigDecimal("74585"), result.getIsee());
    }
}