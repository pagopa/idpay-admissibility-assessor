package it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op;

import it.gov.pagopa.admissibility.drools.model.filter.Filter;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.common.drools.utils.DroolsTemplateRuleUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** It will build the value to check in case of scalar operation.
 * @see #scalarOperations */
@Service
@Slf4j
public class ScalarOpValueBuilder implements OperationValueBuilder{
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss[.SSS]]");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm[:ss[.SSS]]");

    private static final Set<FilterOperator> scalarOperations= Set.of(
            FilterOperator.EQ,
            FilterOperator.NOT_EQ,
            FilterOperator.LE,
            FilterOperator.LT,
            FilterOperator.GE,
            FilterOperator.GT,
            FilterOperator.BTW_CLOSED,
            FilterOperator.BTW_OPEN
    );

    @Override
    public boolean supports(FilterOperator operator) {
        return scalarOperations.contains(operator);
    }

    @Override
    public String[] apply(Filter filter, Class<?> fieldType, Map<String, Object> context) {
        try {
            String v1 = DroolsTemplateRuleUtils.toTemplateParam(deserializeValue(filter.getValue(), fieldType)).getParam();
            if(filter.getValue2()!=null){
                return new String[]{v1, DroolsTemplateRuleUtils.toTemplateParam(deserializeValue(filter.getValue2(), fieldType)).getParam()};
            } else {
                return new String[]{v1};
            }
        } catch (Exception e) {
            log.error("Something gone wrong analyzing the input extraFilter", e);
            throw new IllegalArgumentException(
                    String.format("Unsupported value provided for the field %s: it is supposed to be a %s", filter.getField(), fieldType));
        }
    }

    /**
     * it will deserialize the input String
     */
    private Object deserializeValue(String value, Class<?> fieldType) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if(value == null){
            return null;
        } else if(String.class.isAssignableFrom(fieldType)){
            return value;
        } else if(Boolean.class.isAssignableFrom(fieldType) || boolean.class.isAssignableFrom(fieldType)){
            return Boolean.valueOf(value);
        } else if (Number.class.isAssignableFrom(fieldType)){
            return fieldType.getConstructor(String.class).newInstance(value);
        } else if (int.class.isAssignableFrom(fieldType)){
            return Integer.parseInt(value);
        } else if (float.class.isAssignableFrom(fieldType)){
            return Float.parseFloat(value);
        } else if (double.class.isAssignableFrom(fieldType)){
            return Double.parseDouble(value);
        } else if (LocalDateTime.class.isAssignableFrom(fieldType)){
            return LocalDateTime.parse(value, dateTimeFormatter);
        } else if (LocalDate.class.isAssignableFrom(fieldType)){
            return LocalDate.parse(value, dateFormatter);
        } else if (LocalTime.class.isAssignableFrom(fieldType)){
            return LocalTime.parse(value, timeFormatter);
        }  else if (Enum.class.isAssignableFrom(fieldType)){
            //noinspection unchecked,rawtypes
            return Enum.valueOf((Class<? extends Enum>)fieldType, value);
        } else if (Collection.class.isAssignableFrom(fieldType)){
            return handleCollection(value, fieldType);
        } else {
            return deserializeValueZone(value, fieldType);
        }
    }

    private Object deserializeValueZone(String value, Class<?> fieldType) {
        if (ZonedDateTime.class.isAssignableFrom(fieldType)){
            return ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        } else if (OffsetDateTime.class.isAssignableFrom(fieldType)){
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } else if (ZoneOffset.class.isAssignableFrom(fieldType)){
            return ZoneOffset.of(value);
        } else if (ZoneId.class.isAssignableFrom(fieldType)){
            return ZoneId.of(value);
        }else {
            throw new IllegalArgumentException(String.format("Unsupported scalar value: %s", value.getClass()));
        }
    }

    private Object handleCollection(String value, Class<?> fieldType) {
        if(value.startsWith("(") && value.endsWith(")")){
            List<String> values = Arrays.asList(value.substring(1, value.length()-1).split(","));
            if(Set.class.isAssignableFrom(fieldType)){
                return new HashSet<>(values);
            } else if (List.class.isAssignableFrom(fieldType)){
                return values;
            } else {
                throw new IllegalStateException(String.format("Unsupported collection type %s", fieldType));
            }
        } else {
            return value;
        }
    }
}
