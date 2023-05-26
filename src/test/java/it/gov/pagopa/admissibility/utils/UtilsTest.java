package it.gov.pagopa.admissibility.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
class UtilsTest {

    @Test
    void testBirthDateFromFiscalCodeAndAge() {
        Map<String, LocalDate> mapFiscalCodesBirthDates = Map.of(
                "PTRGNL73S51X000Q", LocalDate.of(1973, 11, 11),
                "BLBGRC57A05X000D", LocalDate.of(1957, 1, 5),
                "DSUFLV00R19X000P", LocalDate.of(2000, 10, 19),
                "BCCVCR01E65X000T", LocalDate.of(2001, 5, 25)
        );

        for(String fc : mapFiscalCodesBirthDates.keySet()) {
            LocalDate result = Utils.calculateBirthDateFromFiscalCode(fc);

            Assertions.assertEquals(mapFiscalCodesBirthDates.get(fc), result);
        }

        int age = Utils.getAge(LocalDate.now().minusYears(20).plusDays(1));
        Assertions.assertEquals(19, age);
    }
}
