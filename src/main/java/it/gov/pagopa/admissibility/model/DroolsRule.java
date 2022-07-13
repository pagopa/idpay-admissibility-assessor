package it.gov.pagopa.admissibility.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DroolsRule {
    private String name;
    private String agendaGroup;
    private String ruleCondition;
    private String ruleConsequence;
}
