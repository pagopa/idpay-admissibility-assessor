package it.gov.pagopa.admissibility.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "initiative_counters")
@FieldNameConstants()
public class InitiativeCounters {
    @Id
    private String id;
    private BigDecimal initiativeBudget;

    @Builder.Default
    private Long onboarded=0L;
    @Builder.Default
    private BigDecimal reservedInitiativeBudget=BigDecimal.ZERO;
}
