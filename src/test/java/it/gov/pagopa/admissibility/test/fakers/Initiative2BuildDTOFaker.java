package it.gov.pagopa.admissibility.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.dto.rule.*;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import it.gov.pagopa.admissibility.utils.TestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class Initiative2BuildDTOFaker {
    private Initiative2BuildDTOFaker(){}

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService());

    /** It will return an example of {@link Initiative2BuildDTO}. Providing a bias, it will return a pseudo-casual object */
    public static Initiative2BuildDTO mockInstance(Integer bias){
        return mockInstanceBuilder(bias).build();
    }
    public static Initiative2BuildDTO.Initiative2BuildDTOBuilder mockInstanceBuilder(Integer bias){
        Initiative2BuildDTO.Initiative2BuildDTOBuilder out = Initiative2BuildDTO.builder();

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.initiativeId(fakeValuesService.bothify(bias!=null? "id_%d".formatted(bias) : "?????"));
        out.initiativeName(fakeValuesService.bothify("?????"));
        out.organizationId(fakeValuesService.bothify("?????"));
        out.status(fakeValuesService.bothify(bias!=null? "status_%d".formatted(bias) : "?????"));
        out.initiativeRewardType("REFUND");

        final InitiativeBeneficiaryRuleDTO beneficiaryRule = new InitiativeBeneficiaryRuleDTO();
        List<IseeTypologyEnum> typology = List.of(IseeTypologyEnum.UNIVERSITARIO, IseeTypologyEnum.ORDINARIO);
        beneficiaryRule.setAutomatedCriteria(new ArrayList<>());
        beneficiaryRule.getAutomatedCriteria().add(new AutomatedCriteriaDTO("AUTH1", CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE, null, FilterOperator.GT, "10", null, null, typology));
        beneficiaryRule.getAutomatedCriteria().add(new AutomatedCriteriaDTO("AUTH2", CriteriaCodeConfigFaker.CRITERIA_CODE_BIRTHDATE, "year", FilterOperator.GT, "10", null, null, typology));

        out.beneficiaryRule(beneficiaryRule);

        out.pdndToken("PDND_TOKEN");
        out.general(
                InitiativeGeneralDTO.builder()
                        .name("NAME")
                        .budget(new BigDecimal("100000.00"))
                        .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                        .beneficiaryKnown(Boolean.TRUE)
                        .beneficiaryBudget(new BigDecimal("1000.00"))
                        .startDate(LocalDate.of(2021, 1, 1))
                        .endDate(LocalDate.of(2025, 12, 1))
                        .rankingEnabled(false)
                        .build());


        out.additionalInfo(new InitiativeAdditionalInfoDTO(
                "SERVICENAME%s".formatted(bias),
                "ARGUMENT%s".formatted(bias),
                "DESCRIPTION%s".formatted(bias),
                List.of(ChannelsDTO.builder().type("web").contact("CONTACT%s".formatted(bias)).build())
        ));

        TestUtils.checkNotNullFields(out.build());
        return out;
    }

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(new Random(bias)));
    }
}
