package it.gov.pagopa.admissibility.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ResultVerifyDTO {

    private String code;
    private boolean resultVerify;


}
