package it.gov.pagopa.admissibility.dto.onboarding.mapper;

import it.gov.pagopa.admissibility.dto.rule.beneficiary.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.rest.initiative.dto.InitiativeDTO;
import it.gov.pagopa.admissibility.rest.initiative.dto.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class InitiativeDTO2ConfigMapperTest {

    @Test
    void testInitiativeDTO2ConfigOk() {

        // Given
//        InitiativeDTO initiativeDTO = Mockito.mock(InitiativeDTO.class);
        InitiativeDTO initiativeDTO = new InitiativeDTO(
                "1",
                "Test",
                "Org",
                "PdndToken",
                "OK",
                true,
                true,
                InitiativeGeneralDTO
                        .builder()
                        .startDate(LocalDate.of(2022, 1, 1))
                        .endDate(LocalDate.of(2022, 12, 31))
                        .budget(new BigDecimal(100000.00))
                        .beneficiaryBudget(new BigDecimal(1000.00))
                        .build()
                ,
                InitiativeBeneficiaryRuleDTO
                        .builder()
                        .automatedCriteria(Arrays.asList(
                        AutomatedCriteriaDTO.builder().code("1").build(),
                        AutomatedCriteriaDTO.builder().code("2").build(),
                        AutomatedCriteriaDTO.builder().code("3").build()))
                        .build()
        );

        InitiativeDTO2ConfigMapper initiativeDTO2ConfigMapper = new InitiativeDTO2ConfigMapper();

        List<String> automatedCriteriaCodesMock = Arrays.asList("1", "2", "3");

//      Mockito.when(initiativeDTO.getInitiativeId()).thenReturn("1");
//      Mockito.when(initiativeDTO.getGeneral().getStartDate()).thenReturn(LocalDate.of(2022, 1, 1));
//      Mockito.when(initiativeDTO.getGeneral().getEndDate()).thenReturn(LocalDate.of(2022, 12, 31));
//      Mockito.when().thenReturn()

        // When
        InitiativeConfig result = initiativeDTO2ConfigMapper.apply(initiativeDTO);

        // Then
        assertEquals("1", result.getInitiativeId());
        assertEquals(LocalDate.of(2022, 1, 1), result.getStartDate());
        assertEquals(LocalDate.of(2022, 12, 31), result.getEndDate());
        assertEquals(automatedCriteriaCodesMock, result.getAutomatedCriteriaCodes());

        TestUtils.checkNotNullFields(result);
    }

    @Test
    void testInitiativeDTO2ConfigKo() {

        // Given
//        InitiativeDTO initiativeDTO = Mockito.mock(InitiativeDTO.class);
        InitiativeDTO initiativeDTO = new InitiativeDTO(
                null,
                "Test",
                "Org",
                "PdndToken",
                "OK",
                true,
                true,
                InitiativeGeneralDTO
                        .builder()
                        .startDate(LocalDate.of(2022, 1, 1))
                        .endDate(LocalDate.of(2022, 12, 31))
                        .build()
                ,
                InitiativeBeneficiaryRuleDTO
                        .builder()
                        .automatedCriteria(Arrays.asList(
                                AutomatedCriteriaDTO.builder().code("1").build(),
                                AutomatedCriteriaDTO.builder().code("2").build(),
                                AutomatedCriteriaDTO.builder().code("3").build()))
                        .build()
        );

        InitiativeDTO2ConfigMapper initiativeDTO2ConfigMapper = new InitiativeDTO2ConfigMapper();

        List<String> automatedCriteriaCodesMock = Arrays.asList("1", "2", "3");

//      Mockito.when(initiativeDTO.getInitiativeId()).thenReturn("1");
//      Mockito.when(initiativeDTO.getGeneral().getStartDate()).thenReturn(LocalDate.of(2022, 1, 1));
//      Mockito.when(initiativeDTO.getGeneral().getEndDate()).thenReturn(LocalDate.of(2022, 12, 31));
//      Mockito.when().thenReturn()

        // When
        InitiativeConfig result = initiativeDTO2ConfigMapper.apply(initiativeDTO);

        // Then
        assertNull(result.getInitiativeId());
        assertEquals(LocalDate.of(2022, 1, 1), result.getStartDate());
        assertEquals(LocalDate.of(2022, 12, 31), result.getEndDate());
        assertEquals(automatedCriteriaCodesMock, result.getAutomatedCriteriaCodes());
    }
}