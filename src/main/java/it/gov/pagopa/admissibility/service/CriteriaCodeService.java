package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;

/** This component will return the criteria code configuration  */
public interface CriteriaCodeService {
    CriteriaCodeConfig getCriteriaCodeConfig(String criteriaCode);
}
