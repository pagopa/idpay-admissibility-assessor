package it.gov.pagopa.admissibility.drools.transformer.extra_filter.not;

import it.gov.pagopa.admissibility.drools.model.NotOperation;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformer;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformerFacade;

import java.util.Map;

public class NotOperation2DroolsTransformer implements ExtraFilter2DroolsTransformer<NotOperation> {
    public String apply(ExtraFilter2DroolsTransformerFacade extraFilter2DroolsTransformerFacade, NotOperation notOperation, Class<?> entityClass, Map<String, Object> context) {
        if(notOperation != null){
            return "!("+ extraFilter2DroolsTransformerFacade.apply(notOperation.getFilter(), entityClass, context)+")";
        } else {
            return "";
        }
    }
}
