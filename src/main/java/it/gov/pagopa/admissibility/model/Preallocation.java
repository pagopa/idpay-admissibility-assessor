package it.gov.pagopa.admissibility.model;

import it.gov.pagopa.admissibility.enums.PreallocationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class Preallocation {

    private String userId;
    private PreallocationStatus status;
    private LocalDateTime createdAt;
    private Long sequenceNumber;
    private LocalDateTime enqueuedTime;
}
