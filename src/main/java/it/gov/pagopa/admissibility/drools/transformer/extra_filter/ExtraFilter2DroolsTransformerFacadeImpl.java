package it.gov.pagopa.admissibility.drools.transformer.extra_filter;

import it.gov.pagopa.admissibility.drools.model.ExtraFilter;
import it.gov.pagopa.admissibility.drools.model.NotOperation;
import it.gov.pagopa.admissibility.drools.model.aggregator.Aggregator;
import it.gov.pagopa.admissibility.drools.model.filter.Filter;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.aggregator.Aggregator2DroolsTransformer;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.Filter2DroolsTransformer;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.not.NotOperation2DroolsTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @see ExtraFilter2DroolsTransformerFacade
 */
@Service
public class ExtraFilter2DroolsTransformerFacadeImpl implements ExtraFilter2DroolsTransformerFacade {
    private final Aggregator2DroolsTransformer aggregator2DroolsTransformer;
    private final NotOperation2DroolsTransformer notOperation2DroolsTransformer;
    private final Filter2DroolsTransformer filter2DroolsTransformer;

    @Autowired
    public ExtraFilter2DroolsTransformerFacadeImpl() {
        this.aggregator2DroolsTransformer = new Aggregator2DroolsTransformer();
        this.notOperation2DroolsTransformer = new NotOperation2DroolsTransformer();
        this.filter2DroolsTransformer = new Filter2DroolsTransformer();
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
        if (extraFilter instanceof Aggregator aggregator) {
            droolsConditionBuilder.append(aggregator2DroolsTransformer.apply(this, aggregator, entityClass, context));
        } else if (extraFilter instanceof NotOperation notOperation) {
            droolsConditionBuilder.append(notOperation2DroolsTransformer.apply(this, notOperation, entityClass, context));
        } else if (extraFilter instanceof Filter filter) {
            droolsConditionBuilder.append(filter2DroolsTransformer.apply(this, filter, entityClass, context));
        }
        return droolsConditionBuilder.toString();
    }
}
