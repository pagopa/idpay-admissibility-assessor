package it.gov.pagopa.admissibility.drools.model.filter;

import it.gov.pagopa.admissibility.drools.model.ExtraFilter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/** to express a filter */
@Data @NoArgsConstructor @AllArgsConstructor
public class Filter implements ExtraFilter {
    @NotNull
    private String field;
    @NotNull
    private FilterOperator filterOperator;
    @NotNull
    private String value;
}
