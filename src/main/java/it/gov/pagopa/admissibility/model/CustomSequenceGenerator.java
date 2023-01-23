package it.gov.pagopa.admissibility.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "custom_sequence")
@FieldNameConstants()
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomSequenceGenerator {
    @Id
    private String sequenceId;
    private long value;
}