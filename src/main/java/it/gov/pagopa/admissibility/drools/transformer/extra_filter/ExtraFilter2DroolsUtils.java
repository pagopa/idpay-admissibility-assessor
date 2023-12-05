package it.gov.pagopa.admissibility.drools.transformer.extra_filter;

import it.gov.pagopa.admissibility.drools.model.ExtraFilterField;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
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

        ReflectionUtils.doWithMethods(clazz, m -> checkIfGetter2AnalyzeAndRetrieveFieldInfo(m, class2subclass, path2ignore, maxDepth, out, fieldsAdded,
                        generateExtraFilterField(path, castPath)),
                m -> m.getParameterTypes().length == 0 && (m.getName().startsWith("get") || m.getName().startsWith("is")));
        return out;
    }

    private static ExtraFilterField generateExtraFilterField(String path, Class<?> castPath){
        ExtraFilterField eff = new ExtraFilterField();
        eff.setPath(path);
        eff.setCastPath(castPath);
        return eff;
    }

    private static void checkIfGetter2AnalyzeAndRetrieveFieldInfo(Method m, Map<Class<?>, List<Class<?>>> class2subclass, Set<String> path2ignore, int maxDepth, List<ExtraFilterField> out, Set<String> fieldsAdded, ExtraFilterField eff) {
        eff.setType(m.getReturnType());
        if (class2notAnalyze.stream().anyMatch(c -> c.isAssignableFrom(eff.getType()))) {
            return;
        }

        eff.setName(StringUtils.uncapitalize(m.getName().replaceFirst("^(?:get|is)", "")));
        eff.setField(eff.getPath() != null ? String.format("%s.%s", eff.getPath(), eff.getName()) : eff.getName());
        if (!Modifier.isStatic(m.getModifiers()) && !fieldsAdded.contains(eff.getField()) && (path2ignore == null || !path2ignore.contains(eff.getField()))) {
            extractFieldInfo(class2subclass, path2ignore, maxDepth, out, fieldsAdded, eff);
        }
    }

    private static void extractFieldInfo(Map<Class<?>, List<Class<?>>> class2subclass, Set<String> path2ignore, int maxDepth, List<ExtraFilterField> out, Set<String> fieldsAdded, ExtraFilterField eff) {
        out.add(eff);
        fieldsAdded.add(eff.getField());

        List<Class<?>> subclasses = class2subclass == null ? null : class2subclass.get(eff.getType());
        if (!CollectionUtils.isEmpty(subclasses)) {
            eff.setToCast(true);
            eff.setSubclasses(subclasses);
            for (Class<?> s : subclasses) {
                if (eff.getType().isAssignableFrom(s)) {
                    out.addAll(buildExtraFilterFields(s, String.format("%s(%s)%s", eff.getPath() == null ? "" : String.format("%s.", eff.getPath()), s.getName(), eff.getName()), s, class2subclass, path2ignore, maxDepth));
                } else {
                    throw new IllegalArgumentException(String.format("The configured class '%s' is not a subclass of '%s'", s.getName(), eff.getType()));
                }
            }
        } else if (!eff.getType().isPrimitive() && class2notExplore.stream().noneMatch(c -> c.isAssignableFrom(eff.getType()))) {
            out.addAll(buildExtraFilterFields(eff.getType(), eff.getField(), null, class2subclass, path2ignore, maxDepth));
        }
    }
}
