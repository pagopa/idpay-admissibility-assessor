package it.gov.pagopa.admissibility.dto.onboarding.extra;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BirthDate {
    private String year;
    private Integer age;
}
