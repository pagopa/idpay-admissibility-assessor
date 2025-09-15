package it.gov.pagopa.admissibility.dto.rule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

/**
 * InitiativeGeneralDTO
 */
@Validated
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2022-07-10T13:24:21.794Z[GMT]")

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class InitiativeGeneralDTO   {

  @JsonProperty("name")
  private String name;

  @JsonProperty("budgetCents")
  private Long budgetCents;

  @JsonProperty("beneficiaryType")
  private BeneficiaryTypeEnum beneficiaryType;

  @JsonProperty("beneficiaryKnown")
  private Boolean beneficiaryKnown;

  @JsonProperty("beneficiaryBudgetCents")
  private Long beneficiaryBudgetCents;

  @JsonProperty("beneficiaryBudgetMaxCents")
  private Long beneficiaryBudgetMaxCents;

  @JsonProperty("startDate")
  private LocalDate startDate;

  @JsonProperty("endDate")
  private LocalDate endDate;

  @JsonProperty("rankingStartDate")
  private LocalDate rankingStartDate;

  @JsonProperty("rankingEndDate")
  private LocalDate rankingEndDate;

  @JsonProperty("rankingEnabled")
  private boolean rankingEnabled;

  /**
   * Gets or Sets beneficiaryType
   */
  public enum BeneficiaryTypeEnum {
    /** Individual (Persona Fisica) */
    PF("PF"),
    /** Legal Person (Persona Giuridica) */
    PG("PG"),
    /** Family (Nucleo Familiare) */
    NF("NF");

    private String value;

    BeneficiaryTypeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static BeneficiaryTypeEnum fromValue(String text) {
      for (BeneficiaryTypeEnum b : BeneficiaryTypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }
}
