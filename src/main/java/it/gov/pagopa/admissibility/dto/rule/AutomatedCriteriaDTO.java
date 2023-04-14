package it.gov.pagopa.admissibility.dto.rule;

import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import java.util.List;
import lombok.*;
import org.springframework.data.domain.Sort;

/**
 * AutomatedCriteriaDTO
 */
@Data @AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class AutomatedCriteriaDTO   {
    private String authority;
    private String code;
    private String field;
    private FilterOperator operator;
    private String value;
    private String value2;
    private Sort.Direction orderDirection;
    private List<IseeTypologyEnum> iseeTypes;
}
