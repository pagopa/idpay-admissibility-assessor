package it.gov.pagopa.admissibility.drools.transformer.extra_filter;

import it.gov.pagopa.admissibility.drools.model.ExtraFilter;

import java.util.Map;

/** A service to validate and tranform an{@link ExtraFilter} into a {DroolsRule#getRuleCondition()} using as context entity the input <i>entityClass</i> */
public interface ExtraFilter2DroolsTransformerFacade {
    /**
     * @param context: it will be used to store information used during parser (Initialized as default to new HashMap).
     *               Filters aggregated using AND will share the same context, Filters aggregated using OR will use separated context (inherited from parent)
     * @see ExtraFilter2DroolsTransformerFacade */
    String apply(ExtraFilter extraFilter, Class<?> entityClass, Map<String, Object> context);
}
