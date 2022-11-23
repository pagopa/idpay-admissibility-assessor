package it.gov.pagopa.admissibility.drools.model.aggregator;

import it.gov.pagopa.admissibility.drools.model.ExtraFilter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/** To aggregate using AND */
@Data @NoArgsConstructor @AllArgsConstructor
public class AggregatorAnd implements Aggregator {
    @NotEmpty
    private List<ExtraFilter> operands;
}
