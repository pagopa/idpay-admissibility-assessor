package it.gov.pagopa.admissibility.drools.transformer.extra_filter;

import it.gov.pagopa.admissibility.drools.model.ExtraFilter;

import java.util.Map;

public interface ExtraFilter2DroolsTransformer <T extends ExtraFilter> {
     String apply(ExtraFilter2DroolsTransformerFacade transformerFacade, T extraFilter, Class<?> entityClass, Map<String, Object> context);
}
