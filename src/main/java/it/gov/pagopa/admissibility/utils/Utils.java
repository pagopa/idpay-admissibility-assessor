package it.gov.pagopa.admissibility.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.Period;

@Slf4j
public final class Utils {
    private Utils(){}

    public static final String FISCAL_CODE_STRUCTURE_REGEX = "^([A-Za-z]{6}[0-9lmnpqrstuvLMNPQRSTUV]{2}[abcdehlmprstABCDEHLMPRST][0-9lmnpqrstuvLMNPQRSTUV]{2}[A-Za-z][0-9lmnpqrstuvLMNPQRSTUV]{3}[A-Za-z])$";
    public static final String FISCAL_CODE_MONTH_LETTERS = "ABCDEHLMPRST";

    //region birthdate from fiscalcode
    public static LocalDate calculateBirthDateFromFiscalCode(String fiscalCode) {
        if (!fiscalCode.matches(FISCAL_CODE_STRUCTURE_REGEX)) {
            throw new IllegalArgumentException("[ADMISSIBILITY] Fiscal code is not valid!");
        }

        // Extract birthdate characters from the fiscal code
        String birthDateCode = fiscalCode.substring(6, 11);

        // Extract birth year, month, and day from the code
        int birthYearDigits = parseCfDigits(birthDateCode.substring(0, 2));
        char birthMonthCode = birthDateCode.charAt(2);
        int birthDay = parseCfDigits(birthDateCode.substring(3));

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

    private static int parseCfDigits(String cfNumericField) {
        return Integer.parseInt(
                cfNumericField
                        .replace('L', '0')
                        .replace('M', '1')
                        .replace('N', '2')
                        .replace('P', '3')
                        .replace('Q', '4')
                        .replace('R', '5')
                        .replace('S', '6')
                        .replace('T', '7')
                        .replace('U', '8')
                        .replace('V', '9')
        );
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
