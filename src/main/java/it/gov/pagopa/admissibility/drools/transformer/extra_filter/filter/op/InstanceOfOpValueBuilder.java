package it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op;

import it.gov.pagopa.admissibility.drools.model.filter.Filter;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/** It will build the value to check in case of {@link FilterOperator#INSTANCE_OF} */
@Service
public class InstanceOfOpValueBuilder implements OperationValueBuilder {
    @Override
    public boolean supports(FilterOperator operator) {
        return FilterOperator.INSTANCE_OF.equals(operator);
    }

    @Override
    public String[] apply(Filter filter, Class<?> fieldType, Map<String, Object> context) {
        try {
            Class<?> class2Check = Class.forName(filter.getValue());
            if (!fieldType.isAssignableFrom(class2Check)) {
                throw new IllegalArgumentException(String.format("Unsupported Class provided for the field %s", filter.getField()));
            } else {
                ExtraFilter2DroolsUtils.storeFieldType(filter.getField(), class2Check, context);
                return new String[]{class2Check.getName()};
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(String.format("Unknown Class provided for the field %s", filter.getField()));
        }
    }
}
