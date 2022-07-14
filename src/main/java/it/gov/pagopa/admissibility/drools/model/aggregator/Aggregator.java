package it.gov.pagopa.admissibility.drools.model.aggregator;

import it.gov.pagopa.admissibility.drools.model.ExtraFilter;

import java.util.List;

/** To aggregate more filters */
public interface Aggregator extends ExtraFilter {
    List<ExtraFilter> getOperands();
    void setOperands(List<ExtraFilter> operands);
}
