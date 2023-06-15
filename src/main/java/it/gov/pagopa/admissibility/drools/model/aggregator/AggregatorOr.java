package it.gov.pagopa.admissibility.drools.model.aggregator;

import it.gov.pagopa.admissibility.drools.model.ExtraFilter;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** To aggregate using OR */
@Data @NoArgsConstructor @AllArgsConstructor
public class AggregatorOr implements Aggregator {
    @NotEmpty
    private List<ExtraFilter> operands;
}
