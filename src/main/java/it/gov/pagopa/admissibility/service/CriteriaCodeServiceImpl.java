package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.config.CriteriaCodesConfiguration;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CriteriaCodeServiceImpl implements CriteriaCodeService {

    @Autowired
    private CriteriaCodesConfiguration criteriaCodesConfiguration;

    @Override
    public CriteriaCodeConfig getCriteriaCodeConfig(String criteriaCode) {
        return criteriaCodesConfiguration.getCriteriaCodeConfigs().get(criteriaCode);
    }
}
