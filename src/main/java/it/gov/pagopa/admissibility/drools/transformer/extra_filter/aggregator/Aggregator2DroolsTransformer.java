package it.gov.pagopa.admissibility.drools.transformer.extra_filter.aggregator;

import it.gov.pagopa.admissibility.drools.model.aggregator.Aggregator;
import it.gov.pagopa.admissibility.drools.model.aggregator.AggregatorAnd;
import it.gov.pagopa.admissibility.drools.model.aggregator.AggregatorOr;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformer;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformerFacade;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Aggregator2DroolsTransformer implements ExtraFilter2DroolsTransformer<Aggregator> {

    public String apply(ExtraFilter2DroolsTransformerFacade extraFilter2DroolsTransformerFacade, Aggregator aggregator, Class<?> entityClass, Map<String, Object> context) {
        if (!CollectionUtils.isEmpty(aggregator.getOperands())) {
            String aggregationOp = transcodeAggregatorOp(aggregator);
            Map<String, Object> aggregatorContext;
            if(aggregator instanceof AggregatorAnd){
                aggregatorContext = new HashMap<>(context);
            } else {
                aggregatorContext = context;
            }
            return String.format("(%s)", aggregator.getOperands().stream()
                    .map(o -> extraFilter2DroolsTransformerFacade.apply(o, entityClass, (aggregator instanceof AggregatorOr)?new HashMap<>(context):aggregatorContext))
                    .collect(Collectors.joining(aggregationOp)));
        } else {
            return "";
        }
    }

    /**
     * It will determine the Drools aggregator operator starting from {@link Aggregator}
     */
    private String transcodeAggregatorOp(Aggregator aggregator) {
        if (aggregator instanceof AggregatorAnd) {
            return " && ";
        } else if (aggregator instanceof AggregatorOr) {
            return " || ";
        } else {
            throw new IllegalArgumentException(String.format("Unsupported Aggregator operator:%s", aggregator.getClass()));
        }
    }
}
