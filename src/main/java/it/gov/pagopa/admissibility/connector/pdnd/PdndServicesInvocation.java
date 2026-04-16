package it.gov.pagopa.admissibility.connector.pdnd;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * DTO minimale che rappresenta una singola invocazione PDND
 * per una specifica verifica.
 *
 * Non contiene logica di dominio (ISEE, RESIDENCE, ecc.),
 * ma solo le informazioni necessarie all’invocazione.
 */
@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class PdndServicesInvocation {

    /**
     * Codice del criterio (es. ISEE, RESIDENCE, BIRTHDATE, ...)
     */
    private final String code;

    /**
     * Indica se è richiesta una verifica esterna
     */
    private final boolean verify;

    /**
     * Codice soglia da verificare (può essere null)
     */
    private final String thresholdCode;

    /**
     * Indica se è necessario invocare PDND
     */
    public boolean requirePdndInvocation() {
        return verify;
    }
}