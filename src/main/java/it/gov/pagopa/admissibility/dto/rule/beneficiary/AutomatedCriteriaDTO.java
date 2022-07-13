package it.gov.pagopa.admissibility.dto.rule.beneficiary;

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

    private Boolean field;

    private String operator;

    private String value;
}
