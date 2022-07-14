package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.springframework.stereotype.Service;

@Service
public class CriteriaCodeServiceImpl implements CriteriaCodeService {

    @Override
    public CriteriaCodeConfig getCriteriaCodeConfig(String criteriaCode) {// TODO
        CriteriaCodeConfig mockedCriteriaCodeConfig = new CriteriaCodeConfig();
        mockedCriteriaCodeConfig.setAuthority("AUTHORITY1");
        mockedCriteriaCodeConfig.setOnboardingField("isee");
        return mockedCriteriaCodeConfig;
    }
}
