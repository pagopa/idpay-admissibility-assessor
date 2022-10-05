package it.gov.pagopa.admissibility.service.build.filter;

import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.test.fakers.Initiative2BuildDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BeneficiaryListFilterTest {

    @Test
    void test1() {
        // Given
        BeneficiaryListFilter beneficiaryListFilter = new BeneficiaryListFilter();

        Initiative2BuildDTO initiativeWithBeneficiaryRule = Initiative2BuildDTOFaker.mockInstance(1);
        Initiative2BuildDTO initiativeWithBeneficiaryList = Initiative2BuildDTOFaker.mockInstance(2);
        initiativeWithBeneficiaryList.setBeneficiaryRule(null);

        // When
        boolean resultWithBeneficiaryRule = beneficiaryListFilter.test(initiativeWithBeneficiaryRule);
        boolean resultWithBeneficiaryList = beneficiaryListFilter.test(initiativeWithBeneficiaryList);

        // Then
        Assertions.assertTrue(resultWithBeneficiaryRule);
        Assertions.assertFalse(resultWithBeneficiaryList);

    }
}