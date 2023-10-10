package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.drools.model.ExtraFilter;
import it.gov.pagopa.admissibility.drools.model.NotOperation;
import it.gov.pagopa.admissibility.drools.model.filter.Filter;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.function.BiFunction;

@Service
public class AutomatedCriteria2ExtraFilterMapper implements BiFunction<AutomatedCriteriaDTO, CriteriaCodeConfig, ExtraFilter> {
    @Override
    public ExtraFilter apply(AutomatedCriteriaDTO automatedCriteriaDTO, CriteriaCodeConfig criteriaCodeConfig) {
        String field = String.format("%s%s", criteriaCodeConfig.getOnboardingField(), StringUtils.isEmpty(automatedCriteriaDTO.getField()) ? "" : ".%s".formatted(automatedCriteriaDTO.getField()));
        FilterOperator operator = automatedCriteriaDTO.getOperator();
        return new NotOperation(new Filter(field, operator, toUpperCase(automatedCriteriaDTO.getValue()), toUpperCase(automatedCriteriaDTO.getValue2())));
    }

    @Nullable
    private static String toUpperCase(String value) {
        return value != null ? value.toUpperCase() : null;
    }
}
