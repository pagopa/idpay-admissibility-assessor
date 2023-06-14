package it.gov.pagopa.admissibility.drools.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** To negate an {@link ExtraFilter} */
@Data @NoArgsConstructor @AllArgsConstructor
public class NotOperation implements ExtraFilter {
    @NotNull
    private ExtraFilter filter;
}
