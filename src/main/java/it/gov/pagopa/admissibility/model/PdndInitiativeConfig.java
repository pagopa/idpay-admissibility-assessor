package it.gov.pagopa.admissibility.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
public class PdndInitiativeConfig {
    private String clientId;
    private String kid;
    private String purposeId;
}
