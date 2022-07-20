package it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op;

import it.gov.pagopa.admissibility.drools.model.filter.Filter;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InOpValueBuilder implements OperationValueBuilder{

    private final ScalarOpValueBuilder scalarOpValueBuilder;

    @Autowired
    public InOpValueBuilder(ScalarOpValueBuilder scalarOpValueBuilder) {
        this.scalarOpValueBuilder = scalarOpValueBuilder;
    }

    @Override
    public boolean supports(FilterOperator operator) {
        return FilterOperator.IN.equals(operator);
    }

    @Override
    public String apply(Filter filter, Class<?> fieldType, Map<String, Object> context) {
        String[] values = filter.getValue().replaceFirst("^\\(", "").replaceFirst("\\)$", "").split(",");

        return String.format("(%s)", Arrays.stream(values).map(v -> scalarOpValueBuilder.apply(new Filter(null, null, v), fieldType, context)).collect(Collectors.joining(",")));
    }
}
