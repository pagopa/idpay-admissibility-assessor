package it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter;

import it.gov.pagopa.admissibility.drools.model.filter.Filter;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformer;
import it.gov.pagopa.admissibility.model.DroolsRule;

import java.util.Map;

/**
 * To transform an {@link Filter} into {@link DroolsRule#getRuleCondition()}
 */
public interface Filter2DroolsTranformer {
    /** @see Filter2DroolsTranformer */
    String apply(ExtraFilter2DroolsTransformer extraFilter2DroolsTransformer, Filter filter, Class<?> entityClass, Map<String, Object> context);
}
