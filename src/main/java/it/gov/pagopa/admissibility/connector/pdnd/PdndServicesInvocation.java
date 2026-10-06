package it.gov.pagopa.admissibility.connector.pdnd;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class PdndServicesInvocation {

    private final String code;          // es. ISEE, RESIDENCE, BIRTHDATE, ...
    private final boolean verify;       // se la verifica va eseguita
    private final String thresholdCode; // può essere null

    public boolean requirePdndInvocation() {
        return verify;
    }
}
