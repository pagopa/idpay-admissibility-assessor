package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.rule.*;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.Order;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.common.utils.TestUtils;
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
        Assertions.assertTrue(result.getIsLogoPresent());

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
        Assertions.assertFalse(result.getIsLogoPresent());

        TestUtils.checkNotNullFields(result,"serviceId");
    }

    @Test
    void testRankingFalse() {
        Initiative2BuildDTO initiative2BuildDTO = initDto();
        initiative2BuildDTO.getGeneral().setRankingEnabled(Boolean.FALSE);

        setAdditionalInfo(initiative2BuildDTO);
        initiative2BuildDTO.getAdditionalInfo().setLogoFileName("");

        final InitiativeConfig result = initiative2InitiativeConfigMapper.apply(initiative2BuildDTO);

        Assertions.assertNotNull(result);

        commonAssertions(initiative2BuildDTO,result);
        Assertions.assertEquals(List.of("CODE1", "CODE2", "CODE3"), result.getAutomatedCriteriaCodes());
        Assertions.assertEquals(Boolean.FALSE, result.isRankingInitiative());
        Assertions.assertFalse(result.getIsLogoPresent());

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
        Assertions.assertTrue(result.getIsLogoPresent());

        TestUtils.checkNotNullFields(result, "automatedCriteria", "automatedCriteriaCodes", "apiKeyClientId", "apiKeyClientAssertion");
    }

    private void setAdditionalInfo(Initiative2BuildDTO initiative2BuildDTO) {
        initiative2BuildDTO.setAdditionalInfo(InitiativeAdditionalInfoDTO.builder()
                .serviceName("SERVICENAME")
                .argument("ARGUMENT")
                .description("DESCRIPTION")
                .channels(List.of(
                        ChannelsDTO.builder().type("web").contact("contact").build()
                ))
                .logoFileName("test.png")
                .build());
    }

    private Initiative2BuildDTO initDto() {
        Initiative2BuildDTO initiative2BuildDTO = new Initiative2BuildDTO();
        initiative2BuildDTO.setInitiativeId("INITIATIVEID");
        initiative2BuildDTO.setInitiativeName("INITIATIVENAME");
        initiative2BuildDTO.setOrganizationId("ORGANIZATIONID");
        initiative2BuildDTO.setOrganizationName("ORGANIZATIONNAME");
        initiative2BuildDTO.setStatus("STATUS");
        initiative2BuildDTO.setInitiativeRewardType("REFUND");

        initiative2BuildDTO.setGeneral(InitiativeGeneralDTO.builder()
                .startDate(LocalDate.MIN)
                .endDate(LocalDate.MAX)
                .budget(BigDecimal.TEN)
                .beneficiaryBudget(BigDecimal.ONE)
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                .build());

        initiative2BuildDTO.setBeneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                .automatedCriteria(List.of(
                        AutomatedCriteriaDTO.builder().code("CODE1").orderDirection(Sort.Direction.ASC).pdndConfig(new PdndInitiativeConfig("CLIENTID1", "KID1", "PURPOSEID1")).build(),
                        AutomatedCriteriaDTO.builder().code("CODE2").orderDirection(Sort.Direction.DESC).pdndConfig(new PdndInitiativeConfig("CLIENTID2", "KID2", "PURPOSEID2")).build(),
                        AutomatedCriteriaDTO.builder().code("CODE3").pdndConfig(new PdndInitiativeConfig("CLIENTID3", "KID3", "PURPOSEID3")).build()
                ))
                .build());

        return initiative2BuildDTO;

    }

    private void commonAssertions(Initiative2BuildDTO initiative2BuildDTO, InitiativeConfig result) {
        Assertions.assertSame(initiative2BuildDTO.getInitiativeId(), result.getInitiativeId());
        Assertions.assertSame(initiative2BuildDTO.getInitiativeName(), result.getInitiativeName());
        Assertions.assertSame(initiative2BuildDTO.getOrganizationId(), result.getOrganizationId());
        Assertions.assertSame(initiative2BuildDTO.getOrganizationName(), result.getOrganizationName());
        Assertions.assertSame(initiative2BuildDTO.getGeneral().getStartDate(), result.getStartDate());
        Assertions.assertSame(initiative2BuildDTO.getGeneral().getEndDate(), result.getEndDate());
        Assertions.assertSame(initiative2BuildDTO.getGeneral().getBudget(), result.getInitiativeBudget());
        Assertions.assertSame(initiative2BuildDTO.getGeneral().getBeneficiaryBudget(), result.getBeneficiaryInitiativeBudget());
        Assertions.assertSame(initiative2BuildDTO.getGeneral().getBeneficiaryType(), result.getBeneficiaryType());
        Assertions.assertSame(initiative2BuildDTO.getStatus(), result.getStatus());
        Assertions.assertSame(initiative2BuildDTO.getBeneficiaryRule().getAutomatedCriteria(), result.getAutomatedCriteria());
        Assertions.assertSame(initiative2BuildDTO.getInitiativeRewardType(), result.getInitiativeRewardType());
    }
}
