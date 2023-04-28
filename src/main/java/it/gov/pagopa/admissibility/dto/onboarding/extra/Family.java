package it.gov.pagopa.admissibility.dto.onboarding.extra;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Family {
    private String familyId;
    private Set<String> memberIds;
}
