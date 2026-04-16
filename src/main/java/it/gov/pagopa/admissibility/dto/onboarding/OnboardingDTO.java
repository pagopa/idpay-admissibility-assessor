package it.gov.pagopa.admissibility.dto.onboarding;


import it.gov.pagopa.admissibility.dto.onboarding.extra.BirthDate;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Family;
import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.enums.OnboardingEvaluationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDTO {

    // Identificazione
    private String userId;
    private String initiativeId;
    private String serviceId;

    // Stato onboarding
    private OnboardingEvaluationStatus status;
    private boolean tc;
    private Boolean pdndAccept;
    private boolean budgetReserved;

    private LocalDateTime tcAcceptTimestamp;
    private LocalDateTime criteriaConsensusTimestamp;

    // Authorities data (popolate solo se richieste)
    private BigDecimal isee;
    private Residence residence;
    private BirthDate birthDate;
    private Family family;

    // Verifiche configurate sull’iniziativa
    private List<VerifyDTO> verifies;

    // Esito delle verifiche effettuate
    private List<ResultVerifyDTO> resultsVerifies;

    // Budget fisso iniziativa (se presente)
    private Long beneficiaryBudgetFixedCents;

    // Dati utente
    private String userMail;
    private String channel;
    private String name;
    private String surname;


    public void addResultVerify(String code, boolean result) {
        if (resultsVerifies == null) {
            resultsVerifies = new ArrayList<>();
        }
        resultsVerifies.add(new ResultVerifyDTO(code, result));
    }
}