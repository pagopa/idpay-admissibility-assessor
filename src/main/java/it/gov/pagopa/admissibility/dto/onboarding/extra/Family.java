package it.gov.pagopa.admissibility.dto.onboarding.extra;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Family {
    private String familyId;
    private Set<String> memberIds;
}
