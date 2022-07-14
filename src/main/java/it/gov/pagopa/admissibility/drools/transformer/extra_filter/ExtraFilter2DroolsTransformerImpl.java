package it.gov.pagopa.admissibility.drools.transformer.extra_filter;

import it.gov.pagopa.admissibility.drools.model.ExtraFilter;
import it.gov.pagopa.admissibility.drools.model.NotOperation;
import it.gov.pagopa.admissibility.drools.model.aggregator.Aggregator;
import it.gov.pagopa.admissibility.drools.model.filter.Filter;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.aggregator.Aggregator2DroolsTransformer;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.Filter2DroolsTranformer;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.not.NotOperation2DroolsTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @see ExtraFilter2DroolsTransformer
 */
@Service
public class ExtraFilter2DroolsTransformerImpl implements ExtraFilter2DroolsTransformer {
    private final Aggregator2DroolsTransformer aggregator2DroolsTransformer;
    private final NotOperation2DroolsTransformer notOperation2DroolsTransformer;
    private final Filter2DroolsTranformer filter2DroolsTranformer;

    @Autowired
    public ExtraFilter2DroolsTransformerImpl(Aggregator2DroolsTransformer aggregator2DroolsTransformer, NotOperation2DroolsTransformer notOperation2DroolsTransformer, Filter2DroolsTranformer filter2DroolsTranformer) {
        this.aggregator2DroolsTransformer = aggregator2DroolsTransformer;
        this.notOperation2DroolsTransformer = notOperation2DroolsTransformer;
        this.filter2DroolsTranformer = filter2DroolsTranformer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String apply(ExtraFilter extraFilter, Class<?> entityClass, Map<String, Object> context) {
        if(context == null){
            context = new HashMap<>();
        }
        StringBuilder droolsConditionBuilder = new StringBuilder();
        if (extraFilter instanceof Aggregator) {
            droolsConditionBuilder.append(aggregator2DroolsTransformer.apply(this, (Aggregator) extraFilter, entityClass, context));
        } else if (extraFilter instanceof NotOperation) {
            droolsConditionBuilder.append(notOperation2DroolsTransformer.apply(this, (NotOperation) extraFilter, entityClass, context));
        } else if (extraFilter instanceof Filter) {
            droolsConditionBuilder.append(filter2DroolsTranformer.apply(this, (Filter) extraFilter, entityClass, context));
        }
        return droolsConditionBuilder.toString();
    }
}
