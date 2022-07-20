package it.gov.pagopa.admissibility.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.dto.build.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.utils.TestUtils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public final class Initiative2BuildDTOFaker {
    private Initiative2BuildDTOFaker(){}

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService());

    /** It will return an example of {@link Initiative2BuildDTO}. Providing a bias, it will return a pseudo-casual object */
    public static Initiative2BuildDTO mockInstance(Integer bias){
        Initiative2BuildDTO out = new Initiative2BuildDTO();

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.setInitiativeId(fakeValuesService.bothify(bias!=null? "id_%d".formatted(bias) : "?????"));
        out.setInitiativeName(fakeValuesService.bothify("?????"));

        out.setBeneficiaryRule(new InitiativeBeneficiaryRuleDTO());
        out.getBeneficiaryRule().setAutomatedCriteria(new ArrayList<>());
        out.getBeneficiaryRule().getAutomatedCriteria().add(new AutomatedCriteriaDTO("AUTH1", CriteriaCodeConfigFaker.CRITERIA_CODE_ISEE, null, FilterOperator.GT, "10"));
        out.getBeneficiaryRule().getAutomatedCriteria().add(new AutomatedCriteriaDTO("AUTH2", CriteriaCodeConfigFaker.CRITERIA_CODE_BIRTHDATE, "anno", FilterOperator.GT, "10"));

        TestUtils.checkNotNullFields(out);
        return out;
    }

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(new Random(bias)));
    }
}
