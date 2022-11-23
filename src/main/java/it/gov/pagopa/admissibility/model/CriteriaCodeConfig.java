package it.gov.pagopa.admissibility.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The configuration of a single criteria code */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaCodeConfig {
    private String code;
    private String authority;
    private String authorityLabel;
    private String onboardingField;
}
