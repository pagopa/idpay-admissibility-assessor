package it.gov.pagopa.admissibility.connector.rest.anpr.mapper;

import it.gov.pagopa.admissibility.dto.onboarding.extra.Residence;
import it.gov.pagopa.admissibility.generated.openapi.pdnd.residence.assessment.client.dto.TipoResidenzaDTO;
import org.springframework.stereotype.Service;

@Service
public class TipoResidenzaDTO2ResidenceMapper {

    public Residence apply(TipoResidenzaDTO residenzaDTO) {
        Residence out = new Residence();

        out.setPostalCode(residenzaDTO.getIndirizzo().getCap());
        out.setProvince(residenzaDTO.getIndirizzo().getComune().getSiglaProvinciaIstat().toUpperCase());
        out.setCity(residenzaDTO.getIndirizzo().getComune().getNomeComune().toUpperCase());
        out.setCityCouncil(residenzaDTO.getIndirizzo().getComune().getNomeComune().toUpperCase());
        // TODO nation, region

        return out;
    }
}
