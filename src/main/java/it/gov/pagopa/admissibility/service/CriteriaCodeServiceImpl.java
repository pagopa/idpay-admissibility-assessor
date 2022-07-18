package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.springframework.stereotype.Service;

@Service
public class CriteriaCodeServiceImpl implements CriteriaCodeService {

    @Override
    public CriteriaCodeConfig getCriteriaCodeConfig(String criteriaCode) {// TODO actually we will read from ConfigMap, next using Spring cloud kubernetes?
        CriteriaCodeConfig mockedCriteriaCodeConfig = new CriteriaCodeConfig();
        mockedCriteriaCodeConfig.setCode(criteriaCode);
        mockedCriteriaCodeConfig.setAuthority("AUTHORITY1");
        mockedCriteriaCodeConfig.setOnboardingField("isee");
        return mockedCriteriaCodeConfig;
    }
}
