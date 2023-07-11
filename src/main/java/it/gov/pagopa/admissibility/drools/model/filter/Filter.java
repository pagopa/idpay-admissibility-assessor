package it.gov.pagopa.admissibility.drools.model.filter;

import it.gov.pagopa.admissibility.drools.model.ExtraFilter;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** to express a filter */
@Data
public class Filter implements ExtraFilter {
    @NotNull
    private String field;
    @NotNull
    private FilterOperator filterOperator;
    @NotNull
    private String value;
    /** used just when {@link FilterOperator#BTW_OPEN} and {@link FilterOperator#BTW_CLOSED} */
    private String value2;


    public Filter() {
    }

    public Filter(@NotNull String field, @NotNull FilterOperator filterOperator, @NotNull String value) {
        this(field, filterOperator, value, null);
    }

    public Filter(@NotNull String field, @NotNull FilterOperator filterOperator, @NotNull String value, String value2) {
        this.field = field;
        this.filterOperator = filterOperator;
        this.value = value;
        this.value2 = value2;
    }
}
