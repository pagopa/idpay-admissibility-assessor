package it.gov.pagopa.admissibility.drools.transformer.extra_filter.not;

import it.gov.pagopa.admissibility.drools.model.NotOperation;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformer;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotOperation2DroolsTransformerImpl implements NotOperation2DroolsTransformer {
    @Override
    public String apply(ExtraFilter2DroolsTransformer extraFilter2DroolsTransformer, NotOperation notOperation, Class<?> entityClass, Map<String, Object> context) {
        if(notOperation != null){
            return "!("+ extraFilter2DroolsTransformer.apply(notOperation.getFilter(), entityClass, context)+")";
        } else {
            return "";
        }
    }
}
