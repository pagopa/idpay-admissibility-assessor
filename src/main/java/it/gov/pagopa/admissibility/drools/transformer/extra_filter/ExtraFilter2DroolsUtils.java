package it.gov.pagopa.admissibility.drools.transformer.extra_filter;

import it.gov.pagopa.admissibility.drools.model.ExtraFilterField;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

public class ExtraFilter2DroolsUtils {
    private ExtraFilter2DroolsUtils() {
    }

    public static final String FIELD_TYPE_PREFIX_KEY = "FIELD_TYPE_";

    /**
     * To store inside the context the Class to associate to a field
     */
    public static void storeFieldType(String field, Class<?> fieldType, Map<String, Object> context) {
        context.put(buildFieldTypeKey(field), fieldType);
    }

    /**
     * To retrieve from the context the Class to associate to a field
     */
    public static Class<?> retrieveFieldType(String field, Map<String, Object> context) {
        Object fieldTypeContext = context.get(buildFieldTypeKey(field));
        if (fieldTypeContext instanceof Class<?>) {
            return (Class<?>) fieldTypeContext;
        } else {
            return null;
        }
    }

    /**
     * To build the key to use to store/retrieve the Class associated to a field
     */
    public static String buildFieldTypeKey(String field) {
        return String.format("%s%s", FIELD_TYPE_PREFIX_KEY, field);
    }

    /**
     * To retrieve from the context the Class to associate to a field
     *
     * @param clazz:          the class to analyze
     * @param class2subclass: a map containing for each superclass, all the subclass to be considered
     * @param path2ignore:    a list of path to ignore
     * @return list of {@link ExtraFilterField} to be provided as ExtraFilter
     */
    public static List<ExtraFilterField> buildExtraFilterFields(final Class<?> clazz, final Map<Class<?>, List<Class<?>>> class2subclass, final Set<String> path2ignore) {
        return buildExtraFilterFields(clazz, null, null, class2subclass, path2ignore, 50);
    }

    private static final Set<Class<?>> class2notExplore = new HashSet<>(Arrays.asList(
            String.class,
            Number.class,
            Iterable.class
    ));
    private static final Set<Class<?>> class2notAnalyze = new HashSet<>(Arrays.asList(
            Map.class,
            Type.class
    ));
    private static List<ExtraFilterField> buildExtraFilterFields(final Class<?> clazz, final String path, final Class<?> castPath, final Map<Class<?>, List<Class<?>>> class2subclass, final Set<String> path2ignore, final int maxDepth) {
        if (StringUtils.countMatches(path, '.') == maxDepth) {
            return Collections.emptyList();
        }

        final List<ExtraFilterField> out = new ArrayList<>();
        final Set<String> fieldsAdded = new HashSet<>();

        ReflectionUtils.doWithMethods(clazz, m -> {
                    Class<?> fieldType = m.getReturnType();
                    if(class2notAnalyze.stream().anyMatch(c->c.isAssignableFrom(fieldType))){
                        return;
                    }

                    String fieldName = StringUtils.uncapitalize(m.getName().replaceFirst("^(?:get|is)", ""));
                    String fullFieldName = path != null ? String.format("%s.%s", path, fieldName) : fieldName;
                    if (!Modifier.isStatic(m.getModifiers()) && !fieldsAdded.contains(fullFieldName) && (path2ignore == null || !path2ignore.contains(fullFieldName))) {
                        ExtraFilterField eff = new ExtraFilterField();
                        eff.setPath(path);
                        eff.setName(fieldName);
                        eff.setField(fullFieldName);
                        eff.setType(fieldType);
                        eff.setCastPath(castPath);

                        out.add(eff);
                        fieldsAdded.add(fullFieldName);

                        List<Class<?>> subclasses = class2subclass == null ? null : class2subclass.get(fieldType);
                        if (!CollectionUtils.isEmpty(subclasses)) {
                            eff.setToCast(true);
                            eff.setSubclasses(subclasses);
                            for (Class<?> s : subclasses) {
                                if (fieldType.isAssignableFrom(s)) {
                                    out.addAll(buildExtraFilterFields(s, String.format("%s(%s)%s", path == null ? "" : String.format("%s.", path), s.getName(), fieldName), s, class2subclass, path2ignore, maxDepth));
                                } else {
                                    throw new IllegalArgumentException(String.format("The configured class '%s' is not a subclass of '%s'", s.getName(), fieldType));
                                }
                            }
                        } else if (!fieldType.isPrimitive() && class2notExplore.stream().noneMatch(c-> c.isAssignableFrom(fieldType))) {
                            out.addAll(buildExtraFilterFields(fieldType, fullFieldName, null, class2subclass, path2ignore, maxDepth));
                        }
                    }
                },
                m -> m.getParameterTypes().length == 0 && (m.getName().startsWith("get") || m.getName().startsWith("is")));
        return out;
    }
}
