package it.gov.pagopa.admissibility.dto.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.model.PdndInitiativeConfig;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

class Initiative2BuildDTOTest {

    @Test
    void testDeserialization() throws JsonProcessingException {

        // given
        String payload = "{"
                + "\"initiativeId\":\"62f12c58f85da16207b3c593\","
                + "\"initiativeName\":\"Ball\","
                + "\"organizationId\":\"2f63a151-da4e-4e1e-acf9-adecc0c4d727\","
                + "\"status\":\"DRAFT\","
                + "\"general\":{"
                + "  \"budgetCents\":6151486,"
                + "  \"beneficiaryType\":\"PF\","
                + "  \"beneficiaryKnown\":false,"
                + "  \"beneficiaryBudgetFixedCents\":38100,"
                + "  \"startDate\":\"2022-10-08\","
                + "  \"endDate\":\"2022-11-08\","
                + "  \"rankingStartDate\":\"2022-08-08\","
                + "  \"rankingEndDate\":\"2022-09-08\""
                + "},"
                + "\"additionalInfo\":{"
                + "  \"serviceName\":\"Tuna\","
                + "  \"argument\":\"Velit mollitia consequatur aut mollitia quas modi a quia.\","
                + "  \"description\":\"Totam sit aspernatur natus est accusantium ad quisquam architecto.\","
                + "  \"channels\":[{\"type\":\"web\",\"contact\":\"http://jayme.name\"}]"
                + "},"
                + "\"beneficiaryRule\":{"
                + "  \"selfDeclarationCriteria\":["
                + "    {\"_type\":\"boolean\",\"description\":\"Modi tempora nesciunt excepturi doloribus accusamus corporis ut.\",\"value\":false,\"code\":\"1\"},"
                + "    {\"_type\":\"multi\",\"description\":\"Et aut consequatur at.\",\"value\":[\"Shoes\",\"Games\",\"Jewelery\"],\"code\":\"2\"}"
                + "  ],"
                + "  \"automatedCriteria\":["
                + "    {\"authority\":\"INPS\",\"code\":\"ISEE\",\"operator\":\"BTW_OPEN\",\"value\":\"6.62\",\"value2\":\"54.8\","
                + "     \"pdndConfig\":{\"clientId\":\"CLIENTID\",\"kid\":\"KID\",\"purposeId\":\"PURPOSEID_ISEE\"}},"
                + "    {\"authority\":\"AUTH2\",\"code\":\"RESIDENCE\",\"field\":\"city\",\"operator\":\"IN\",\"value\":\"Roma\","
                + "     \"pdndConfig\":{\"clientId\":\"CLIENTID\",\"kid\":\"KID\",\"purposeId\":\"PURPOSEID_RESIDENCE\"}},"
                + "    {\"authority\":\"AUTH1\",\"code\":\"BIRTHDATE\",\"field\":\"year\",\"operator\":\"LT\",\"value\":\"1801\","
                + "     \"pdndConfig\":{\"clientId\":\"CLIENTID\",\"kid\":\"KID\",\"purposeId\":\"PURPOSEID_BIRTHDATE\"}}"
                + "  ],"
                + "  \"apiKeyClientId\":\"apiKeyClientId\","
                + "  \"apiKeyClientAssertion\":\"apiKeyClientAssertion\""
                + "}"
                + "}";

        // when
        Initiative2BuildDTO result =
                TestUtils.objectMapper.readValue(payload, Initiative2BuildDTO.class);

        // then – top level
        Assertions.assertEquals("62f12c58f85da16207b3c593", result.getInitiativeId());
        Assertions.assertEquals("Ball", result.getInitiativeName());
        Assertions.assertEquals("2f63a151-da4e-4e1e-acf9-adecc0c4d727", result.getOrganizationId());
        Assertions.assertEquals("DRAFT", result.getStatus());

        // general
        InitiativeGeneralDTO general = result.getGeneral();
        Assertions.assertEquals(6151486L, general.getBudgetCents());
        Assertions.assertEquals(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF, general.getBeneficiaryType());
        Assertions.assertFalse(general.getBeneficiaryKnown());
        Assertions.assertEquals(38100L, general.getBeneficiaryBudgetFixedCents());
        Assertions.assertEquals(LocalDate.of(2022, 10, 8), general.getStartDate());
        Assertions.assertEquals(LocalDate.of(2022, 11, 8), general.getEndDate());
        Assertions.assertEquals(LocalDate.of(2022, 8, 8), general.getRankingStartDate());
        Assertions.assertEquals(LocalDate.of(2022, 9, 8), general.getRankingEndDate());

        // beneficiaryRule – self declaration
        InitiativeBeneficiaryRuleDTO rule = result.getBeneficiaryRule();
        Assertions.assertEquals(2, rule.getSelfDeclarationCriteria().size());

        // beneficiaryRule – automated criteria
        List<AutomatedCriteriaDTO> automated = rule.getAutomatedCriteria();
        Assertions.assertEquals(3, automated.size());

        AutomatedCriteriaDTO isee = automated.get(0);
        Assertions.assertEquals("ISEE", isee.getCode());
        Assertions.assertEquals(FilterOperator.BTW_OPEN, isee.getOperator());
        Assertions.assertEquals(
                new PdndInitiativeConfig("CLIENTID", "KID", "PURPOSEID_ISEE"),
                isee.getPdndConfig()
        );

        // additional info
        InitiativeAdditionalInfoDTO info = result.getAdditionalInfo();
        Assertions.assertEquals("Tuna", info.getServiceName());
        Assertions.assertEquals(1, info.getChannels().size());
        Assertions.assertEquals("web", info.getChannels().get(0).getType());
    }
}