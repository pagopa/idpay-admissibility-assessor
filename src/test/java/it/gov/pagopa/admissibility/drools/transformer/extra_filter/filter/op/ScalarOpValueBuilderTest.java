package it.gov.pagopa.admissibility.drools.transformer.extra_filter.filter.op;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
@ExtendWith(SpringExtension.class)
class ScalarOpValueBuilderTest {

    private ScalarOpValueBuilder scalarOpValueBuilder;

    @BeforeEach
    void setUp() {
        scalarOpValueBuilder = new ScalarOpValueBuilder();
    }

    @Test
    void testDeserializeValue_LocalDateTime() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String value = "2023-12-31T23:59:59.999";
        Class<?> fieldType = LocalDateTime.class;

        java.lang.reflect.Method deserializeValueMethod = ScalarOpValueBuilder.class.getDeclaredMethod("deserializeValue", String.class, Class.class);
        deserializeValueMethod.setAccessible(true);

        Object result = deserializeValueMethod.invoke(scalarOpValueBuilder, value, fieldType);

        assertEquals(LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss[.SSS]]")), result);

    }
    @Test
    void testDeserializeValue_ZonedDateTime() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String value = "2023-12-31T23:59:59.999+01:00";
        Class<?> fieldType = ZonedDateTime.class;

        java.lang.reflect.Method deserializeValueMethod = ScalarOpValueBuilder.class.getDeclaredMethod("deserializeValue", String.class, Class.class);
        deserializeValueMethod.setAccessible(true);

        Object result = deserializeValueMethod.invoke(scalarOpValueBuilder, value, fieldType);

        assertEquals(ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME), result);

    }

    @Test
    void testDeserializeValueZone_UnsupportedType() throws NoSuchMethodException, IllegalAccessException {
        String value = "2023-12-31T23:59:59.999";
        Class<?> fieldType = String.class;

        java.lang.reflect.Method deserializeValueZoneMethod = ScalarOpValueBuilder.class.getDeclaredMethod("deserializeValueZone", String.class, Class.class);
        deserializeValueZoneMethod.setAccessible(true);

        try {
            deserializeValueZoneMethod.invoke(scalarOpValueBuilder, value, fieldType);
        } catch (InvocationTargetException e) {
            assertEquals(IllegalArgumentException.class, e.getCause().getClass());
        }
    }

}