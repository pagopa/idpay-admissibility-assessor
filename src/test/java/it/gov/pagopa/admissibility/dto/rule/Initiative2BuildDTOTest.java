package it.gov.pagopa.admissibility.dto.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

class Initiative2BuildDTOTest {

    @Test
    void testDeserialization() throws JsonProcessingException {
        //given
        String payload = "{\"initiativeId\":\"62f12c58f85da16207b3c593\",\"initiativeName\":\"Ball\",\"organizationId\":\"2f63a151-da4e-4e1e-acf9-adecc0c4d727\",\"status\":\"DRAFT\",\"pdndToken\":\"TOKEN\",\"creationDate\":\"2022-08-08T15:31:36.572\",\"updateDate\":\"2022-08-08T15:31:41.9943445\",\"general\":{\"budget\":61514.86,\"beneficiaryType\":\"PF\",\"beneficiaryKnown\":false,\"beneficiaryBudget\":381,\"startDate\":\"2022-10-08\",\"endDate\":\"2022-11-08\",\"rankingStartDate\":\"2022-08-08\",\"rankingEndDate\":\"2022-09-08\"},\"additionalInfo\":{\"serviceName\":\"Tuna\",\"argument\":\"Velit mollitia consequatur aut mollitia quas modi a quia.\",\"description\":\"Totam sit aspernatur natus est accusantium ad quisquam architecto.\",\"channels\":[{\"type\":\"web\",\"contact\":\"http://jayme.name\"}]},\"beneficiaryRule\":{\"selfDeclarationCriteria\":[{\"_type\":\"boolean\",\"description\":\"Modi tempora nesciunt excepturi doloribus accusamus corporis ut.\",\"value\":false,\"code\":\"1\"},{\"_type\":\"multi\",\"description\":\"Et aut consequatur at.\",\"value\":[\"Shoes\",\"Games\",\"Jewelery\"],\"code\":\"2\"}],\"automatedCriteria\":[{\"authority\":\"INPS\",\"code\":\"ISEE\",\"operator\":\"BTW_OPEN\",\"value\":\"6.62\",\"value2\":\"54.8\"},{\"authority\":\"AUTH2\",\"code\":\"RESIDENCE\",\"field\":\"city\",\"operator\":\"IN\",\"value\":\"Roma\"},{\"authority\":\"AUTH1\",\"code\":\"BIRTHDATE\",\"field\":\"year\",\"operator\":\"LT\",\"value\":\"1801\"}],\"apiKeyClientId\":\"apiKeyClientId\",\"apiKeyClientAssertion\":\"apiKeyClientAssertion\"},\"rewardRule\":{\"_type\":\"rewardGroups\",\"rewardGroups\":[{\"from\":0.25,\"to\":1,\"rewardValue\":100}]},\"trxRule\":{\"daysOfWeek\":[{\"daysOfWeek\":[\"FRIDAY\"],\"intervals\":[{\"startTime\":\"00:00:00.000\",\"endTime\":\"15:31:41.794\"}]}],\"threshold\":{\"from\":10,\"fromIncluded\":true,\"to\":50,\"toIncluded\":true},\"trxCount\":{\"from\":1,\"fromIncluded\":true,\"to\":3,\"toIncluded\":true},\"mccFilter\":{\"allowedList\":true,\"values\":[\"0743\",\"0744\",\"0742\"]},\"rewardLimits\":[{\"frequency\":\"DAILY\",\"rewardLimit\":3}]}}";
        Initiative2BuildDTO expected = new Initiative2BuildDTO();

        expected.setInitiativeId("62f12c58f85da16207b3c593");
        expected.setInitiativeName("Ball");
        expected.setOrganizationId("2f63a151-da4e-4e1e-acf9-adecc0c4d727");
        expected.setStatus("DRAFT");

        final InitiativeGeneralDTO expectedGeneral = new InitiativeGeneralDTO();
        expectedGeneral.setBudget(BigDecimal.valueOf(61514.86));
        expectedGeneral.setBeneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF);
        expectedGeneral.setBeneficiaryKnown(false);
        expectedGeneral.setBeneficiaryBudget(BigDecimal.valueOf(381));
        expectedGeneral.setStartDate(LocalDate.of(2022, 10, 8));
        expectedGeneral.setEndDate(LocalDate.of(2022, 11, 8));
        expectedGeneral.setRankingStartDate(LocalDate.of(2022, 8, 8));
        expectedGeneral.setRankingEndDate(LocalDate.of(2022, 9, 8));
        expected.setGeneral(expectedGeneral);


        final InitiativeBeneficiaryRuleDTO expectedBeneficiaryRule = new InitiativeBeneficiaryRuleDTO();
        expectedBeneficiaryRule.setSelfDeclarationCriteria(List.of(
                SelfCriteriaBoolDTO.builder().code("1").description("Modi tempora nesciunt excepturi doloribus accusamus corporis ut.").value(false).build(),
                SelfCriteriaMultiDTO.builder().code("2").description("Et aut consequatur at.").value(List.of("Shoes", "Games", "Jewelery")).build()
        ));

        expectedBeneficiaryRule.setAutomatedCriteria(List.of(
                AutomatedCriteriaDTO.builder().code("ISEE").authority("INPS").operator(FilterOperator.BTW_OPEN).value("6.62").value2("54.8").build(),
                AutomatedCriteriaDTO.builder().code("RESIDENCE").field("city").authority("AUTH2").operator(FilterOperator.IN).value("Roma").build(),
                AutomatedCriteriaDTO.builder().code("BIRTHDATE").field("year").authority("AUTH1").operator(FilterOperator.LT).value("1801").build()
        ));

        expectedBeneficiaryRule.setApiKeyClientId("apiKeyClientId");
        expectedBeneficiaryRule.setApiKeyClientAssertion("apiKeyClientAssertion");

        expected.setBeneficiaryRule(expectedBeneficiaryRule);

        final InitiativeAdditionalInfoDTO expectedAdditionalInfo = new InitiativeAdditionalInfoDTO();
        expectedAdditionalInfo.setServiceName("Tuna");
        expectedAdditionalInfo.setArgument("Velit mollitia consequatur aut mollitia quas modi a quia.");
        expectedAdditionalInfo.setDescription("Totam sit aspernatur natus est accusantium ad quisquam architecto.");
        expectedAdditionalInfo.setChannels(List.of(
                ChannelsDTO.builder().type("web").contact("http://jayme.name").build()
        ));

        expected.setAdditionalInfo(expectedAdditionalInfo);

        //when
        final Initiative2BuildDTO result = TestUtils.objectMapper.readValue(payload, Initiative2BuildDTO.class);

        //then
        Assertions.assertEquals(expected, result);
    }
}
