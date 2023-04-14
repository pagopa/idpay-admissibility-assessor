package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.rule.*;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.Order;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

class Initiative2InitiativeConfigMapperTest {
    private final Initiative2InitiativeConfigMapper initiative2InitiativeConfigMapper = new Initiative2InitiativeConfigMapper();

    @Test
    void testAllField() {
        Initiative2BuildDTO initiative2BuildDTO = initDto();
        initiative2BuildDTO.getGeneral().setRankingEnabled(Boolean.TRUE);

        setAdditionalInfo(initiative2BuildDTO);

        final InitiativeConfig result = initiative2InitiativeConfigMapper.apply(initiative2BuildDTO);

        Assertions.assertNotNull(result);

        commonAssertions(initiative2BuildDTO,result);
        Assertions.assertEquals(List.of("CODE1", "CODE2", "CODE3"), result.getAutomatedCriteriaCodes());
        Assertions.assertEquals(Boolean.TRUE, result.isRankingInitiative());
        Assertions.assertEquals(List.of(
                        Order.builder().fieldCode("CODE1").direction(Sort.Direction.ASC).build(),
                        Order.builder().fieldCode("CODE2").direction(Sort.Direction.DESC).build()),
                result.getRankingFields());

        TestUtils.checkNotNullFields(result);
    }

    @Test
    void testAdditionalInfoNull() {
        Initiative2BuildDTO initiative2BuildDTO = initDto();
        initiative2BuildDTO.getGeneral().setRankingEnabled(Boolean.TRUE);

        final InitiativeConfig result = initiative2InitiativeConfigMapper.apply(initiative2BuildDTO);

        Assertions.assertNotNull(result);
        commonAssertions(initiative2BuildDTO, result);
        Assertions.assertEquals(List.of("CODE1", "CODE2", "CODE3"), result.getAutomatedCriteriaCodes());
        Assertions.assertEquals(Boolean.TRUE, result.isRankingInitiative());
        Assertions.assertEquals(
                List.of(
                        Order.builder().fieldCode("CODE1").direction(Sort.Direction.ASC).build(),
                        Order.builder().fieldCode("CODE2").direction(Sort.Direction.DESC).build()),
                result.getRankingFields());

        TestUtils.checkNotNullFields(result,"serviceId");
    }

    @Test
    void testRankingFalse() {
        Initiative2BuildDTO initiative2BuildDTO = initDto();
        initiative2BuildDTO.getGeneral().setRankingEnabled(Boolean.FALSE);

        setAdditionalInfo(initiative2BuildDTO);

        final InitiativeConfig result = initiative2InitiativeConfigMapper.apply(initiative2BuildDTO);

        Assertions.assertNotNull(result);

        commonAssertions(initiative2BuildDTO,result);
        Assertions.assertEquals(List.of("CODE1", "CODE2", "CODE3"), result.getAutomatedCriteriaCodes());
        Assertions.assertEquals(Boolean.FALSE, result.isRankingInitiative());

        TestUtils.checkNotNullFields(result, "rankingFields" );
    }

    @Test
    void testAutomatedCriteriaNull() {
        Initiative2BuildDTO initiative2BuildDTO = initDto();
        initiative2BuildDTO.getGeneral().setRankingEnabled(Boolean.TRUE);

        initiative2BuildDTO.setBeneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                        .selfDeclarationCriteria(List.of(
                                SelfCriteriaBoolDTO.builder().code("CODE1").build()
                        ))
                .build());

        setAdditionalInfo(initiative2BuildDTO);

        final InitiativeConfig result = initiative2InitiativeConfigMapper.apply(initiative2BuildDTO);

        Assertions.assertNotNull(result);

        commonAssertions(initiative2BuildDTO,result);
        Assertions.assertEquals(Boolean.TRUE, result.isRankingInitiative());
        Assertions.assertTrue(result.getRankingFields().isEmpty());

        TestUtils.checkNotNullFields(result, "automatedCriteria", "automatedCriteriaCodes");
    }

    private void setAdditionalInfo(Initiative2BuildDTO initiative2BuildDTO) {
        initiative2BuildDTO.setAdditionalInfo(InitiativeAdditionalInfoDTO.builder()
                .serviceName("SERVICENAME")
                .argument("ARGUMENT")
                .description("DESCRIPTION")
                .channels(List.of(
                        ChannelsDTO.builder().type("web").contact("contact").build()
                ))
                .build());
    }

    private Initiative2BuildDTO initDto() {
        Initiative2BuildDTO initiative2BuildDTO = new Initiative2BuildDTO();
        initiative2BuildDTO.setInitiativeId("INITIATIVEID");
        initiative2BuildDTO.setInitiativeName("INITIATIVENAME");
        initiative2BuildDTO.setOrganizationId("ORGANIZATIONID");
        initiative2BuildDTO.setStatus("STATUS");
        initiative2BuildDTO.setPdndToken("PDNDTOKEN");
        initiative2BuildDTO.setInitiativeRewardType("REFUND");

        initiative2BuildDTO.setGeneral(InitiativeGeneralDTO.builder()
                .startDate(LocalDate.MIN)
                .endDate(LocalDate.MAX)
                .budget(BigDecimal.TEN)
                .beneficiaryBudget(BigDecimal.ONE)
                .build());

        initiative2BuildDTO.setBeneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                .automatedCriteria(List.of(
                        AutomatedCriteriaDTO.builder().code("CODE1").orderDirection(Sort.Direction.ASC).build(),
                        AutomatedCriteriaDTO.builder().code("CODE2").orderDirection(Sort.Direction.DESC).build(),
                        AutomatedCriteriaDTO.builder().code("CODE3").build()
                ))
                .build());

        return initiative2BuildDTO;

    }

    private void commonAssertions(Initiative2BuildDTO initiative2BuildDTO, InitiativeConfig result) {
        Assertions.assertSame(initiative2BuildDTO.getInitiativeId(), result.getInitiativeId());
        Assertions.assertSame(initiative2BuildDTO.getInitiativeName(), result.getInitiativeName());
        Assertions.assertSame(initiative2BuildDTO.getOrganizationId(), result.getOrganizationId());
        Assertions.assertSame(initiative2BuildDTO.getPdndToken(), result.getPdndToken());
        Assertions.assertSame(initiative2BuildDTO.getGeneral().getStartDate(), result.getStartDate());
        Assertions.assertSame(initiative2BuildDTO.getGeneral().getEndDate(), result.getEndDate());
        Assertions.assertSame(initiative2BuildDTO.getGeneral().getBudget(), result.getInitiativeBudget());
        Assertions.assertSame(initiative2BuildDTO.getGeneral().getBeneficiaryBudget(), result.getBeneficiaryInitiativeBudget());
        Assertions.assertSame(initiative2BuildDTO.getStatus(), result.getStatus());
        Assertions.assertSame(initiative2BuildDTO.getBeneficiaryRule().getAutomatedCriteria(), result.getAutomatedCriteria());
    }
}
