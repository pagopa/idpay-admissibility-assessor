package it.gov.pagopa.admissibility.dto.anpr.response;

import it.gov.pagopa.admissibility.enums.PdndResponseType;

public class PdndKoResponse <O,E> extends PdndResponseBase<O, E>{
    private final E error;

    public PdndKoResponse(E error) {
        super(PdndResponseType.KO);
        this.error = error;
    }

    public E getError() {
        return error;
    }

    @Override
    public <R> R accept(PdndResponseVisitor<O, E, R> visitor) {
        return visitor.onKo(error);
    }
}
