package it.gov.pagopa.admissibility.drools.transformer.extra_filter.not;

import it.gov.pagopa.admissibility.drools.model.NotOperation;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformer;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.aggregator.Aggregator2DroolsTransformer;
import it.gov.pagopa.admissibility.model.DroolsRule;

import java.util.Map;

/**
 * To transform an {@link NotOperation} into {@link DroolsRule#getRuleCondition()}
 */
public interface NotOperation2DroolsTransformer {
    /** @see Aggregator2DroolsTransformer */
    String apply(ExtraFilter2DroolsTransformer extraFilter2DroolsTransformer, NotOperation notOperation, Class<?> entityClass, Map<String, Object> context);
}
