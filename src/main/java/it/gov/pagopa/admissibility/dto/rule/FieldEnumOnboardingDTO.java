package it.gov.pagopa.admissibility.dto.rule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Gets or Sets _type
 */
public enum FieldEnumOnboardingDTO {

    BOOLEAN("boolean"),

    MULTI("multi");

    private String value;

    FieldEnumOnboardingDTO(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static FieldEnumOnboardingDTO fromValue(String text) {
        for (FieldEnumOnboardingDTO b : FieldEnumOnboardingDTO.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }
}
