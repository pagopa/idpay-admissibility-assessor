package it.gov.pagopa.admissibility.dto.rule.beneficiary;

import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import lombok.*;

/**
 * AutomatedCriteriaDTO
 */
@Data @AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class AutomatedCriteriaDTO   {

    private String authority;

    private String code ;

    private String field;

    private FilterOperator operator;

    private String value;
}
