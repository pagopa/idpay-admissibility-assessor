package it.gov.pagopa.admissibility.drools.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/** To negate an {@link ExtraFilter} */
@Data @NoArgsConstructor @AllArgsConstructor
public class NotOperation implements ExtraFilter {
    @NotNull
    private ExtraFilter filter;
}
