package it.gov.pagopa.admissibility.service.drools.transformer.admissibility;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.model.DroolsRule;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class BeneficiaryRule2DroolsRuleImpl implements Function<InitiativeBeneficiaryRuleDTO, DroolsRule> {

    @Override
    public DroolsRule apply(InitiativeBeneficiaryRuleDTO beneficiaryRuleDTO) {
        DroolsRule out = new DroolsRule();
        // TODO set out
        out.setName("");
        out.setAgendaGroup("");//id initiative
        out.setRuleCondition(String.format("$omboarding: %S(%S);", OnboardingDroolsDTO.class.getName(), getConditionOnboarding()));
        out.setRuleConsequence("");

        return out;
    }

    private String getConditionOnboarding(){
        return null;
    }

}
