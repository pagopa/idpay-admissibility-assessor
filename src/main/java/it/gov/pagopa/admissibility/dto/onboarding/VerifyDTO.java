package it.gov.pagopa.admissibility.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class VerifyDTO {

    /** Codice della verifica (ISEE, RESIDENCE, ecc.) */
    private String code;

    /** Indica se la verifica deve essere eseguita */
    private boolean verify;

    /** Indica se il fallimento della verifica blocca l’onboarding */
    private boolean blockingVerify;

    /** Codice soglia (es. BELET25), opzionale */
    private String thresholdCode;

    /** Budget minimo assegnabile */
    private Long beneficiaryBudgetCentsMin;

    /** Budget massimo assegnabile */
    private Long beneficiaryBudgetCentsMax;

    /** Esito della verifica (true = OK, false = KO) */
    private boolean resultVerify;
}