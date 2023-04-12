package it.gov.pagopa.admissibility.model;

public enum IseeTypologyEnum {
    ORDINARIO("Ordinario"),
    MINORENNE("Minorenne"),
    UNIVERSITARIO("Universitario"),
    SOCIOSANITARIO("SocioSanitario"),
    DOTTORATO("Dottorato"),
    RESIDENZIALE("Residenziale"),
    CORRENTE("Corrente");

    final String value;
    IseeTypologyEnum(String value) {
        this.value = value;
    }
}
