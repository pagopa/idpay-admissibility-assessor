package it.gov.pagopa.admissibility.dto.onboarding;

import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class VerifyDTO {

    private String code;
    private boolean verify;
    private String thersoldCode;
    private Long beneficiaryBudgetCentsMin;
    private Long beneficiaryBudgetCentsMax;
    private boolean resultVerify;


}
