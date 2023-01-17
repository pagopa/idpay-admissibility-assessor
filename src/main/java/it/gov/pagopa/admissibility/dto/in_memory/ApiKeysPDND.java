package it.gov.pagopa.admissibility.dto.in_memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiKeysPDND {
    private String apiKeyClientId;
    private String apiKeyClientAssertion;
}
