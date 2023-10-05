package it.gov.pagopa.admissibility.connector.pdnd;

import it.gov.pagopa.admissibility.model.IseeTypologyEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@Getter
@ToString
public class PdndServicesInvocation {
    boolean getIsee;
    List<IseeTypologyEnum> iseeTypes;
    boolean getResidence;
    boolean getBirthDate;

    public boolean requirePdndInvocation() {
        return getIsee || getResidence || getBirthDate;
    }
}
