package it.gov.pagopa.admissibility.dto.anpr.response;

public interface PdndResponseVisitor<O,E,R> {
    R onOk(O payload);
    R onKo(E payload);
}
