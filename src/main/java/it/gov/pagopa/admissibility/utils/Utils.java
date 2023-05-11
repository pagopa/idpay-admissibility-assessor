package it.gov.pagopa.admissibility.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import org.springframework.messaging.Message;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.function.Consumer;

public final class Utils {

    public static final String FISCAL_CODE_MONTH_LETTERS = "ABCDEHLMPRST";

    private Utils(){}

    /** It will try to deserialize a message, eventually notifying the error  */
    public static <T> T deserializeMessage(Message<?> message, ObjectReader objectReader, Consumer<Throwable> onError) {
        try {
            String payload = readMessagePayload(message);
            return objectReader.readValue(payload);
        } catch (JsonProcessingException e) {
            onError.accept(e);
            return null;
        }
    }

    public static String readMessagePayload(Message<?> message) {
        String payload;
        if(message.getPayload() instanceof byte[] bytes){
            payload=new String(bytes);
        } else {
            payload= message.getPayload().toString();
        }
        return payload;
    }

    /** To read Message header value */
    @SuppressWarnings("unchecked")
    public static <T> T getHeaderValue(Message<?> message, String headerName) {
        return  (T)message.getHeaders().get(headerName);
    }

    public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    public static Long euro2Cents(BigDecimal euro){
        return euro == null? null : euro.multiply(ONE_HUNDRED).longValue();
    }

    //region birthdate from fiscalcode
    public static LocalDate calculateBirthDateFromFiscalCode(String fiscalCode) {
        // Extract birthdate characters from the fiscal code
        String birthDateCode = fiscalCode.substring(6, 11);

        // Extract birth year, month, and day from the code
        int birthYearDigits = Integer.parseInt(birthDateCode.substring(0, 2));
        char birthMonthCode = birthDateCode.charAt(2);
        int birthDay = Integer.parseInt(birthDateCode.substring(3));

        // Adjust the day for females (increment by 40)
        if (birthDay > 40) {
            birthDay -= 40;
        }

        // Determine the birth year
        int birthYear = calculateBirthYear(birthYearDigits);

        // Determine the birth month from the month code
        int birthMonth = getBirthMonthFromCode(birthMonthCode);

        return LocalDate.of(birthYear, birthMonth, birthDay);
    }

    public static int getAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private static int calculateBirthYear(int birthYearDigits) {
        int currentYear = LocalDate.now().getYear() % 100;
        return birthYearDigits > currentYear ? 1900 + birthYearDigits : 2000 + birthYearDigits;
    }

    private static int getBirthMonthFromCode(char monthCode) {
        int monthIndex = FISCAL_CODE_MONTH_LETTERS.indexOf(Character.toUpperCase(monthCode));
        return monthIndex + 1; // Adding 1 to match the 1-based month indexing in LocalDate
    }
    //endregion
}
