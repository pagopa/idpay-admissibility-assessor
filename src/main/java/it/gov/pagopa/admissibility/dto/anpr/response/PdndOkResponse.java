package it.gov.pagopa.admissibility.dto.anpr.response;

import it.gov.pagopa.admissibility.enums.PdndResponseType;

public class PdndOkResponse <O,E> extends PdndResponseBase<O, E>{
    private final O payload;

    public PdndOkResponse(O payload) {
        super(PdndResponseType.OK);
        this.payload = payload;
    }

    public O getPayload() {
        return payload;
    }

    @Override
    public <R> R accept(PdndResponseVisitor<O, E, R> visitor) {
        return visitor.onOk(payload);
    }
}
