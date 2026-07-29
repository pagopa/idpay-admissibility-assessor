package it.gov.pagopa.admissibility.dto.onboarding;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class VerifyDTO {

    @JsonProperty("code")
    private String code;

    /** indica se la verifica va eseguita */
    @JsonProperty("verify")
    private boolean verify;

    /** indica se il fallimento blocca l’onboarding */
    @JsonProperty("blockingVerify")
    private boolean blockingVerify;

    /** codice soglia (es. BELET25), può essere null */
    @JsonProperty("thresholdCode")
    private String thresholdCode;

    @JsonProperty("beneficiaryBudgetCentsMin")
    private Long beneficiaryBudgetCentsMin;

    @JsonProperty("beneficiaryBudgetCentsMax")
    private Long beneficiaryBudgetCentsMax;

    /** null = non ancora eseguita */
    private List<OnboardingRejectionReason> reasonList;

}