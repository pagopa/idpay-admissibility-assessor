package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.drools.model.ExtraFilter;
import it.gov.pagopa.admissibility.drools.model.NotOperation;
import it.gov.pagopa.admissibility.drools.model.filter.Filter;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AutomatedCriteria2ExtraFilterMapperTest {
    private final AutomatedCriteria2ExtraFilterMapper automatedCriteria2ExtraFilterMapper = new AutomatedCriteria2ExtraFilterMapper();

    @Test
    void test(){
        //given
        AutomatedCriteriaDTO dto = AutomatedCriteriaDTO.builder().code("ISEE").authority("INPS").operator(FilterOperator.BTW_OPEN).value("6.62").value2("54.8").build();
        CriteriaCodeConfig criteriaCodeConfig = new CriteriaCodeConfig();
        criteriaCodeConfig.setCode("ISEE");
        criteriaCodeConfig.setAuthority("INPS");
        criteriaCodeConfig.setOnboardingField("isee");

        ExtraFilter expected = new NotOperation(
                new Filter(
                        "isee",
                        FilterOperator.BTW_OPEN,
                        "6.62",
                        "54.8"
                )
        );

        //when
        final ExtraFilter result = automatedCriteria2ExtraFilterMapper.apply(dto, criteriaCodeConfig);

        //then
        Assertions.assertEquals(expected, result);
    }
}
