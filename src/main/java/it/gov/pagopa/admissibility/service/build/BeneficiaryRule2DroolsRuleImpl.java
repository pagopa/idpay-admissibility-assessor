package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.drools.model.filter.Filter;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformer;
import it.gov.pagopa.admissibility.dto.build.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.stream.Collectors;

@Service
@Slf4j
public class BeneficiaryRule2DroolsRuleImpl implements BeneficiaryRule2DroolsRule {

    private final CriteriaCodeService criteriaCodeService;
    private final ExtraFilter2DroolsTransformer extraFilter2DroolsTransformer;

    public BeneficiaryRule2DroolsRuleImpl(CriteriaCodeService criteriaCodeService, ExtraFilter2DroolsTransformer extraFilter2DroolsTransformer) {
        this.criteriaCodeService = criteriaCodeService;
        this.extraFilter2DroolsTransformer = extraFilter2DroolsTransformer;
    }

    @Override
    public Flux<DroolsRule> apply(Flux<Initiative2BuildDTO> initiativeFlux) {
        return initiativeFlux.map(this::apply);
    }

    private DroolsRule apply(Initiative2BuildDTO initiative) {
        try {
            DroolsRule out = new DroolsRule();
            out.setName(String.format("%s - %s", initiative.getInitiativeId(), initiative.getInitiativeName()));
            out.setAgendaGroup(initiative.getInitiativeId());
            out.setRuleCondition(String.format("$rejectionReasons: new java.util.ArrayList();\n$onboarding: %s();\n%s\n$rejectionReason.size()>0"
                    , OnboardingDroolsDTO.class.getName()
                    , initiative.getBeneficiaryRule().getAutomatedCriteria().stream().map(this::automatedCriteriaBuild).collect(Collectors.joining("\n"))));
            out.setRuleConsequence("$onboarding.getOnboardingRejectionReasons().addAll($rejectionReasons)");

            // TODO test the compilation simulating ad invocation with a container with just this rule?

            return out;
        } catch (RuntimeException e){
            log.error("Something gone wrong while building initiative %s".formatted(initiative.getInitiativeId()), e);
            return null;
        }
    }

    private String automatedCriteriaBuild(AutomatedCriteriaDTO automatedCriteriaDTO){
        CriteriaCodeConfig criteriaCodeConfig = criteriaCodeService.getCriteriaCodeConfig(automatedCriteriaDTO.getCode());
        if(criteriaCodeConfig == null){
            throw new IllegalStateException("Invalid criteria code provided or not configured: %s".formatted(automatedCriteriaDTO.getCode()));
        }
        return "eval(%s ? true : $rejectionReasons.add(\"AUTOMATED_CRITERIA_%s_FAIL\"))".formatted(
                extraFilter2DroolsTransformer.apply(automatedCriteria2ExtraFilter(automatedCriteriaDTO, criteriaCodeConfig), OnboardingDTO.class, null),
                automatedCriteriaDTO.getCode()
        );
    }

    private Filter automatedCriteria2ExtraFilter(AutomatedCriteriaDTO automatedCriteriaDTO, CriteriaCodeConfig criteriaCodeConfig) {
        String field = String.format("%s%s", criteriaCodeConfig.getOnboardingField(), StringUtils.isEmpty(automatedCriteriaDTO.getField()) ? "" : ".%s".formatted(automatedCriteriaDTO.getField()));
        FilterOperator operator = automatedCriteriaDTO.getOperator();
        return new Filter(field, operator, automatedCriteriaDTO.getValue());
    }
}
