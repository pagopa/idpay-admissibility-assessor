package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.config.CriteriaCodesConfiguration;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CriteriaCodeServiceImpl implements CriteriaCodeService {

    @Autowired
    private CriteriaCodesConfiguration criteriaCodesConfiguration;

    @Override
    public CriteriaCodeConfig getCriteriaCodeConfig(String criteriaCode) {
        return criteriaCodesConfiguration.getCriteriaCodeConfigs().get(criteriaCode);
    }
}
