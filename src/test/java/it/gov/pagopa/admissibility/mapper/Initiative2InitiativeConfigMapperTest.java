package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.rule.*;
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
    void testAllField() {
        Initiative2BuildDTO initiative2BuildDTO = initDto();
        initiative2BuildDTO.setRankingInitiative(Boolean.TRUE);

        setAdditionalInfo(initiative2BuildDTO);

        final InitiativeConfig result = initiative2InitiativeConfigMapper.apply(initiative2BuildDTO);

        Assertions.assertNotNull(result);

        commonAssertions(initiative2BuildDTO,result);
        Assertions.assertSame(initiative2BuildDTO.getAdditionalInfo().getServiceId(), result.getServiceId());
        Assertions.assertEquals(Boolean.TRUE, result.getRankingInitiative());
        Assertions.assertEquals(List.of("CODE1", "CODE2"), result.getRankingFieldCodes());

        TestUtils.checkNotNullFields(result);
    }

    @Test
    void testAdditionalInfoNull() {
        Initiative2BuildDTO initiative2BuildDTO = initDto();
        initiative2BuildDTO.setRankingInitiative(Boolean.TRUE);

        final InitiativeConfig result = initiative2InitiativeConfigMapper.apply(initiative2BuildDTO);

        Assertions.assertNotNull(result);

        commonAssertions(initiative2BuildDTO, result);
        Assertions.assertEquals(Boolean.TRUE, result.getRankingInitiative());
        Assertions.assertEquals(List.of("CODE1", "CODE2"), result.getRankingFieldCodes());

        TestUtils.checkNotNullFields(result,"serviceId");
    }

    @Test
    void testSelfDeclarationCriteriaNotBool() {
        Initiative2BuildDTO initiative2BuildDTO = initDto();
        initiative2BuildDTO.setRankingInitiative(Boolean.TRUE);

        initiative2BuildDTO.setBeneficiaryRule(InitiativeBeneficiaryRuleDTO.builder()
                .automatedCriteria(List.of(
                        AutomatedCriteriaDTO.builder().code("CODE1").build(),
                        AutomatedCriteriaDTO.builder().code("CODE2").build(),
                        AutomatedCriteriaDTO.builder().code("CODE3").build()
                ))
                .selfDeclarationCriteria(List.of(
                        SelfCriteriaMultiDTO.builder().code("CODE1").build(),
                        SelfCriteriaMultiDTO.builder().code("CODE2").build()
                ))
                .build());

        setAdditionalInfo(initiative2BuildDTO);

        final InitiativeConfig result = initiative2InitiativeConfigMapper.apply(initiative2BuildDTO);

        Assertions.assertNotNull(result);

        commonAssertions(initiative2BuildDTO,result);
        Assertions.assertSame(initiative2BuildDTO.getAdditionalInfo().getServiceId(), result.getServiceId());
        Assertions.assertEquals(Boolean.TRUE, result.getRankingInitiative());
        Assertions.assertEquals(List.of(), result.getRankingFieldCodes());

        TestUtils.checkNotNullFields(result);
    }

    @Test
    void testRankingFalse() {
        Initiative2BuildDTO initiative2BuildDTO = initDto();
        initiative2BuildDTO.setRankingInitiative(Boolean.FALSE);

        setAdditionalInfo(initiative2BuildDTO);

        final InitiativeConfig result = initiative2InitiativeConfigMapper.apply(initiative2BuildDTO);

        Assertions.assertNotNull(result);

        commonAssertions(initiative2BuildDTO,result);
        Assertions.assertSame(initiative2BuildDTO.getAdditionalInfo().getServiceId(), result.getServiceId());
        Assertions.assertEquals(Boolean.FALSE, result.getRankingInitiative());

        TestUtils.checkNotNullFields(result, "rankingFieldCodes" );
    }

    private void setAdditionalInfo(Initiative2BuildDTO initiative2BuildDTO) {
        initiative2BuildDTO.setAdditionalInfo(InitiativeAdditionalInfoDTO.builder()
                .serviceId("SERVICEID")
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
                .selfDeclarationCriteria(List.of(
                        SelfCriteriaBoolDTO.builder().code("CODE1").value(Boolean.TRUE).build(),
                        SelfCriteriaBoolDTO.builder().code("CODE2").value(Boolean.TRUE).build(),
                        SelfCriteriaBoolDTO.builder().code("CODE3").value(Boolean.FALSE).build()
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
        Assertions.assertEquals(List.of("CODE1", "CODE2", "CODE3"), result.getAutomatedCriteriaCodes());
        Assertions.assertSame(initiative2BuildDTO.getStatus(), result.getStatus());
    }
}
