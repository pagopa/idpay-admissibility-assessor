package it.gov.pagopa.admissibility.drools.model;

import lombok.Value;

@Value
public class DroolsRuleTemplateParam {
    String param;

    @Override
    public String toString(){
        return param;
    }
}
