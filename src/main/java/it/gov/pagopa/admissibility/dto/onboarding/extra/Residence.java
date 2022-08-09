package it.gov.pagopa.admissibility.dto.onboarding.extra;

import lombok.Data;

@Data
public class Residence {
    private String postalCode;
    private String cityCouncil;
    private String province;
    private String city;
    private String region;
    private String nation;
}
