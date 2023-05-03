package it.gov.pagopa.admissibility.dto.onboarding.extra;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldNameConstants
public class Isee {

    @Id
    private String userId;
    private Map<String,BigDecimal> iseeTypeMap;
}
