package it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter;

import it.gov.pagopa.admissibility.drools.model.filter.Filter;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformer;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsUtils;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op.InOpValueBuilder;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op.InstanceOfOpValueBuilder;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op.OperationValueBuilder;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op.ScalarOpValueBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Filter2DroolsTranformer {
    private final List<OperationValueBuilder> operationValueBuilders;

    @Autowired
    public Filter2DroolsTranformer() {
        ScalarOpValueBuilder scalarOpValueBuilder = new ScalarOpValueBuilder();
        this.operationValueBuilders = List.of(
                scalarOpValueBuilder,
                new InstanceOfOpValueBuilder(),
                new InOpValueBuilder(scalarOpValueBuilder));
    }

    public String apply(ExtraFilter2DroolsTransformer extraFilter2DroolsTransformer, Filter filter, Class<?> entityClass, Map<String, Object> context) {
        Class<?> fieldType = determineFieldType(filter, entityClass, context);
        String op = transcodeFilterOp(filter.getFilterOperator(), fieldType, filter.getValue());
        String value = null;
        for (OperationValueBuilder operationValueBuilder : operationValueBuilders) {
            if (operationValueBuilder.supports(filter.getFilterOperator())) {
                value = operationValueBuilder.apply(filter, fieldType, context);
            }
        }

        if (value == null) {
            throw new IllegalArgumentException(String.format("Unsupported filter operation:%s", filter.getFilterOperator()));
        }

        String fullPathNoCast = filter.getField();
        List<String> requiredCasts = new ArrayList<>();
        if(filter.getField().indexOf('(')>-1){
            Matcher matcher = castPattern.matcher(filter.getField());
            while(matcher.find()){
                String cast = matcher.group();
                int startFieldIndex = fullPathNoCast.indexOf(cast);
                int afterCastIndex = startFieldIndex+cast.length();
                int endFieldIndex = fullPathNoCast.indexOf('.',afterCastIndex);
                String field2cast;
                if(endFieldIndex>-1){
                    field2cast = fullPathNoCast.substring(0,startFieldIndex)+fullPathNoCast.substring(afterCastIndex, endFieldIndex);
                }  else {
                    field2cast = fullPathNoCast.substring(0,startFieldIndex)+fullPathNoCast.substring(afterCastIndex);
                }
                requiredCasts.add(String.format("(%s instanceof %s)", field2cast, cast.substring(1,cast.length()-1)));
                fullPathNoCast = fullPathNoCast.replace(cast, "");
            }
        }

        String actualOp = buildOperatorApplication(filter, fieldType, op, value, fullPathNoCast);
        if(requiredCasts.size()>0){
            return String.format("(%s && %s)", String.join(" && ", requiredCasts), actualOp);
        } else {
            return actualOp;
        }
    }

    private String buildOperatorApplication(Filter filter, Class<?> fieldType, String op, String value, String fullPathNoCast) {
        if(fieldType.isAssignableFrom(OffsetDateTime.class) || fieldType.isAssignableFrom(ChronoZonedDateTime.class)){
            switch (filter.getFilterOperator()){
                case EQ: return String.format("%s.isEqual(%s)", fullPathNoCast, value);
                case LT: return String.format("%s.isBefore(%s)", fullPathNoCast, value);
                case LE: return String.format("(%s.isEqual(%s) || %s.isBefore(%s))", fullPathNoCast, value, fullPathNoCast, value);
                case GT: return String.format("%s.isAfter(%s)", fullPathNoCast, value);
                case GE: return String.format("(%s.isEqual(%s) || %s.isAfter(%s))", fullPathNoCast, value, fullPathNoCast, value);
            }
        }

        return String.format("%s %s %s", fullPathNoCast, op, value);
    }

    /**
     * it will navigate {@link Filter#getField()} in order to determine the {@link Class} expected as value
     */
    private static Class<?> determineFieldType(Filter extraFilter, Class<?> entityClass, Map<String, Object> context) {
        Class<?> fieldType = ExtraFilter2DroolsUtils.retrieveFieldType(extraFilter.getField(), context);
        if (fieldType == null) {
            String field = escapeCast(extraFilter.getField());
            String[] conditionField =field.split("\\.");
            String path = null;
            fieldType = entityClass;
            for (String f : conditionField) {
                path = path==null?f: String.format("%s.%s", path, f);
                Class<?> storedType = ExtraFilter2DroolsUtils.retrieveFieldType(path, context);
                if(storedType != null){
                    fieldType=storedType;
                    continue;
                }
                Class<?> baseFieldType = fieldType;
                try {
                    boolean isCasted = f.startsWith("(");
                    Class<?> castedType = null;
                    if(isCasted){
                        int closingCastIndex = f.indexOf(')');
                        String castedClassName = f.substring(1,closingCastIndex).replaceAll("-",".");
                        castedType = Class.forName(castedClassName);
                        f=f.substring(closingCastIndex+1);
                    }

                    //method invocation
                    if (f.contains("()")) {
                        fieldType = baseFieldType.getMethod(f.replaceAll("\\(\\)","")).getReturnType();
                    } else {
                        String capitalizedField = StringUtils.capitalize(f);
                        try {
                            fieldType = baseFieldType.getMethod("get" + capitalizedField).getReturnType();
                        } catch (NoSuchMethodException e) {
                            fieldType = baseFieldType.getMethod("is" + capitalizedField).getReturnType();
                        }
                    }

                    if(castedType!=null){
                        if(!fieldType.isAssignableFrom(castedType)){
                            throw new IllegalArgumentException(
                                    String.format("The Class defined as cast of the field %s of %s is not assignable to field type %s", f, baseFieldType, fieldType.getName()));
                        } else {
                            fieldType = castedType;
                        }
                    }

                    ExtraFilter2DroolsUtils.storeFieldType(path, fieldType, context);
                } catch (NoSuchMethodException e) {
                    throw new IllegalArgumentException(
                            String.format("Cannot find field %s on %s", f, baseFieldType));
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(
                            String.format("Invalid Class defined as cast of the field %s of %s", f, baseFieldType));
                }
            }
        }
        return fieldType;
    }

    private final static Pattern castPattern = Pattern.compile("\\([^)]+\\)");
    private static String escapeCast(String field) {
        String out = field;
        Matcher matcher = castPattern.matcher(field);
        while(matcher.find()){
            String cast = matcher.group();
            String castEscaped = cast.replaceAll("\\.", "-");
            out = out.replace(cast, castEscaped);
        }
        return out;
    }

    public static Class<?> determineFieldType(String field, Class<?> entityClass, Map<String, Object> context) {
        Filter f = new Filter();
        f.setField(field);
        return determineFieldType(f, entityClass, context);
    }

    /**
     * It will determine the Drools operator starting from {@link FilterOperator}
     */
    private String transcodeFilterOp(FilterOperator filterOperator, Class<?> fieldType, @NotNull String value) {
        switch (filterOperator) {
            case EQ:
                if(value != null && Collection.class.isAssignableFrom(fieldType) && !value.startsWith("(") && !value.endsWith(")")){
                    return "contains";
                } else {
                    return "==";
                }
            case NOT_EQ:
                return "!=";
            case GE:
                return ">=";
            case GT:
                return ">";
            case LE:
                return "<=";
            case LT:
                return "<";
            case INSTANCE_OF:
                return "instanceof";
            case IN:
                return "in";
            default:
                throw new IllegalArgumentException(String.format("Unsupported Drools operator:%s", filterOperator));
        }
    }
}
