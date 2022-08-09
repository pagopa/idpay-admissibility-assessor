package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.dto.rule.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.dto.rule.InitiativeGeneralDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

class Initiative2InitiativeConfigMapperTest {
    private final Initiative2InitiativeConfigMapper initiative2InitiativeConfigMapper = new Initiative2InitiativeConfigMapper();

    @Test
    void test() {
        Initiative2BuildDTO initiative2BuildDTO = new Initiative2BuildDTO();
        initiative2BuildDTO.setInitiativeId("INITIATIVEID");
        initiative2BuildDTO.setInitiativeName("INITIATIVENAME");
        initiative2BuildDTO.setOrganizationId("ORGANIZATIONID");
        initiative2BuildDTO.setPdndToken("PDNDTOKEN");

        initiative2BuildDTO.setGeneral(InitiativeGeneralDTO.builder()
                        .startDate(LocalDate.MIN)
                        .endDate(LocalDate.MAX)
                        .budget(BigDecimal.TEN)
                        .beneficiaryBudget(BigDecimal.ONE)
                .build());

        initiative2BuildDTO.setBeneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                        .automatedCriteria(List.of(
                                AutomatedCriteriaDTO.builder().code("CODE1").build(),
                                AutomatedCriteriaDTO.builder().code("CODE2").build(),
                                AutomatedCriteriaDTO.builder().code("CODE3").build()
                        ))
                .build());


        final InitiativeConfig result = initiative2InitiativeConfigMapper.apply(initiative2BuildDTO);

        Assertions.assertNotNull(result);

        Assertions.assertSame(initiative2BuildDTO.getInitiativeId(), result.getInitiativeId());
        Assertions.assertSame(initiative2BuildDTO.getInitiativeName(), result.getInitiativeName());
        Assertions.assertSame(initiative2BuildDTO.getOrganizationId(), result.getOrganizationId());
        Assertions.assertSame(initiative2BuildDTO.getPdndToken(), result.getPdndToken());
        Assertions.assertSame(initiative2BuildDTO.getGeneral().getStartDate(), result.getStartDate());
        Assertions.assertSame(initiative2BuildDTO.getGeneral().getEndDate(), result.getEndDate());
        Assertions.assertSame(initiative2BuildDTO.getGeneral().getBudget(), result.getInitiativeBudget());
        Assertions.assertSame(initiative2BuildDTO.getGeneral().getBeneficiaryBudget(), result.getBeneficiaryInitiativeBudget());
        Assertions.assertEquals(List.of("CODE1", "CODE2", "CODE3"), result.getAutomatedCriteriaCodes());

        TestUtils.checkNotNullFields(result);
    }
}
