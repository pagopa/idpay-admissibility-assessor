package it.gov.pagopa.admissibility.model;

import it.gov.pagopa.admissibility.enums.PreallocationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants()
public class InitiativeCountersPreallocations {

    private String initiativeId;
    private String userId;
    private PreallocationStatus status;
    private LocalDateTime createdAt;
    private Long sequenceNumber;
    private LocalDateTime enqueuedTime;

}
