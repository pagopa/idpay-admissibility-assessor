package it.gov.pagopa.admissibility.dto.onboarding.extra;

import lombok.Data;

@Data
public class Residenza {
    private String citta;
    private String cap;
    private String nazione;
    private String regione;
}
