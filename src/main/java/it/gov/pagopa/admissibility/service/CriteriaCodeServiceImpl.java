package it.gov.pagopa.admissibility.service;

import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CriteriaCodeServiceImpl implements CriteriaCodeService {

    /** mocked criteria codes */
    private static final Map<String, CriteriaCodeConfig> mockedCriteriaCodes = Map.of(
            "ISEE", new CriteriaCodeConfig("ISEE", "INPS", "isee"),
            "BIRTHDATE", new CriteriaCodeConfig("BIRTHDATE", "AGID", "birthDate"),
            "RESIDENZA", new CriteriaCodeConfig("RESIDENZA", "AGID", "residenza")
    );

    @Override
    public CriteriaCodeConfig getCriteriaCodeConfig(String criteriaCode) {// TODO actually we will read from ConfigMap, next using Spring cloud kubernetes?
        return mockedCriteriaCodes.get(criteriaCode);
    }
}
