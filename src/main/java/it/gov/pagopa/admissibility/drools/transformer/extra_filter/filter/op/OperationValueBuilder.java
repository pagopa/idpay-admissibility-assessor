package it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op;

import it.gov.pagopa.admissibility.drools.model.filter.Filter;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;

import java.util.Map;

/** It will build the value field of a Drools condition */
public interface OperationValueBuilder {
    /** It will return true if the FilterOperator is supported */
    boolean supports(FilterOperator operator);

    /** @see OperationValueBuilder */
    String apply(Filter filter, Class<?> fieldType, Map<String, Object> context);
}
