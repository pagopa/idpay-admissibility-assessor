package it.gov.pagopa.admissibility.dto.in_memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgidJwtTokenPayload {
    private String iss;
    private String sub;
    private String aud;
}
