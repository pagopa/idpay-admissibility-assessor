package it.gov.pagopa.admissibility.dto.anpr.response;

import it.gov.pagopa.admissibility.enums.PdndResponseType;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public abstract class PdndResponseBase <O,E> {
    private final PdndResponseType type;
    protected PdndResponseBase(PdndResponseType type) {
        this.type = type;
    }

    public PdndResponseType getType() {
        return type;
    }

    public abstract <R> R accept(PdndResponseVisitor<O, E, R> visitor);
}
