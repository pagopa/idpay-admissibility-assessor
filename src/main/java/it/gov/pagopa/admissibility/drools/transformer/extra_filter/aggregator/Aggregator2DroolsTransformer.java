package it.gov.pagopa.admissibility.drools.transformer.extra_filter.aggregator;

import it.gov.pagopa.admissibility.drools.model.aggregator.Aggregator;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformer;
import it.gov.pagopa.admissibility.model.DroolsRule;

import java.util.Map;

/** A service to validate and tranform an{@link Aggregator} into a {@link DroolsRule#getRuleCondition()} using as context entity the input <i>entityClass</i> */
public interface Aggregator2DroolsTransformer {
    /**
     * @param context: Filters aggregated using AND will share the same context, Filters aggregated using OR will use separated context (inherited from parent)
     * @see Aggregator2DroolsTransformer */
    String apply(ExtraFilter2DroolsTransformer extraFilter2DroolsTransformer, Aggregator aggregator, Class<?> entityClass, Map<String, Object> context);
}
