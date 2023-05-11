package it.gov.pagopa.admissibility.service.onboarding;

import it.gov.pagopa.admissibility.connector.rest.UserRestClient;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.model.Order;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class AuthoritiesDataRetrieverServiceImplTest {

    @Mock
    private OnboardingContextHolderService onboardingContextHolderServiceMock;
    @Mock
    private CriteriaCodeService criteriaCodeServiceMock;
    @Mock
    private ReactiveMongoTemplate reactiveMongoTemplateMock;
    @Mock
    private UserRestClient userRestClientMock;

    private AuthoritiesDataRetrieverService authoritiesDataRetrieverService;

    private OnboardingDTO onboardingDTO;
    private InitiativeConfig initiativeConfig;
    private Message<String> message;

    @BeforeEach
    void setUp() {
        authoritiesDataRetrieverService = new AuthoritiesDataRetrieverServiceImpl(onboardingContextHolderServiceMock, null, 60L, false, criteriaCodeServiceMock, reactiveMongoTemplateMock, userRestClientMock);

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
        List<IseeTypologyEnum> typology = List.of(IseeTypologyEnum.UNIVERSITARIO, IseeTypologyEnum.ORDINARIO);
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
                .automatedCriteria(List.of(new AutomatedCriteriaDTO("AUTH1", "ISEE", null, FilterOperator.EQ, "1", null, Sort.Direction.ASC, typology)))
                .build();

        message = MessageBuilder.withPayload(TestUtils.jsonSerializer(onboardingDTO)).build();

        Mockito.lenient().when(criteriaCodeServiceMock.getCriteriaCodeConfig(Mockito.anyString())).thenReturn(new CriteriaCodeConfig(
                "ISEE",
                "INPS",
                "Istituto Nazionale Previdenza Sociale",
                "ISEE"
        ));

        Mockito.lenient().when(reactiveMongoTemplateMock.findById(Mockito.anyString(), Mockito.any(), Mockito.eq("mocked_isee")))
                .thenReturn(Mono.empty());
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
        Assertions.assertEquals(new BigDecimal("50666"), result.getIsee());
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
        Assertions.assertEquals(new BigDecimal("50666"), result.getIsee());
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
        Assertions.assertEquals("Roma", result.getResidence().getCity());
    }

    @Test
    void retrieveIseeRankingAndNotAutomatedCriteria() {
        // Given
        onboardingDTO.setUserId("USERID2");

        initiativeConfig.setAutomatedCriteriaCodes(List.of("RESIDENCE"));
        initiativeConfig.setRankingFields(List.of(
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_RESIDENCE).direction(Sort.Direction.ASC).build(),
                Order.builder().fieldCode(OnboardingConstants.CRITERIA_CODE_ISEE).direction(Sort.Direction.ASC).build()

        ));

        // When
        OnboardingDTO result = authoritiesDataRetrieverService.retrieve(onboardingDTO, initiativeConfig, message).block();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(new BigDecimal("27589"), result.getIsee());
        Assertions.assertEquals("Milano", result.getResidence().getCity());
    }
}