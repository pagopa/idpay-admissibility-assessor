package it.gov.pagopa.admissibility.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "beneficiary_rule")
public class DroolsRule {
    @Id
    private String name;
    private String agendaGroup;
    private String ruleCondition;
    private String ruleConsequence;
    private String rule;
}
