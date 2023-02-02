package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.config.CriteriaCodesConfiguration;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.springframework.stereotype.Service;

@Service
public class CriteriaCodeServiceImpl implements CriteriaCodeService {

    private final CriteriaCodesConfiguration criteriaCodesConfiguration;

    public CriteriaCodeServiceImpl(CriteriaCodesConfiguration criteriaCodesConfiguration) {
        this.criteriaCodesConfiguration = criteriaCodesConfiguration;
    }

    @Override
    public CriteriaCodeConfig getCriteriaCodeConfig(String criteriaCode) {
        return criteriaCodesConfiguration.getCriteriaCodeConfigs().get(criteriaCode);
    }
}
