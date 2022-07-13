package it.gov.pagopa.admissibility.dto.onboarding.mapper;

import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.rest.initiative.dto.InitiativeDTO;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class InitiativeDTO2Config implements Function<InitiativeDTO, InitiativeConfig> { //TODO to test
    @Override
    public InitiativeConfig apply(InitiativeDTO initiativeDTO) {
        return null; //TODO
    }
}
