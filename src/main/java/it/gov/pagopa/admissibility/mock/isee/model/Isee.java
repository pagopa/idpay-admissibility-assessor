package it.gov.pagopa.admissibility.mock.isee.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldNameConstants
@Document("mocked_isee")
public class Isee {

    @Id
    private String userId;
    private Map<String, BigDecimal> iseeTypeMap;
}