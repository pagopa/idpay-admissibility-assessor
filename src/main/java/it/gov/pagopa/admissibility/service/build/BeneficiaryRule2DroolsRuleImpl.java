package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.drools.model.ExtraFilter;
import it.gov.pagopa.admissibility.drools.model.NotOperation;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.stream.Collectors;

@Service
@Slf4j
public class BeneficiaryRule2DroolsRuleImpl implements BeneficiaryRule2DroolsRule {

    private final boolean onlineSyntaxCheck;

    private final CriteriaCodeService criteriaCodeService;
    private final ExtraFilter2DroolsTransformer extraFilter2DroolsTransformer;
    private final KieContainerBuilderService builderService;

    public BeneficiaryRule2DroolsRuleImpl(@Value("${app.beneficiary-rule.online-syntax-check}") boolean onlineSyntaxCheck, CriteriaCodeService criteriaCodeService, ExtraFilter2DroolsTransformer extraFilter2DroolsTransformer, KieContainerBuilderService builderService) {
        this.onlineSyntaxCheck = onlineSyntaxCheck;
        this.criteriaCodeService = criteriaCodeService;
        this.extraFilter2DroolsTransformer = extraFilter2DroolsTransformer;
        this.builderService = builderService;
    }

    @Override
    public Flux<DroolsRule> apply(Flux<Initiative2BuildDTO> initiativeFlux) {
        return initiativeFlux.map(this::apply);
    }

    private DroolsRule apply(Initiative2BuildDTO initiative) {
        log.info("Building inititative having id: %s".formatted(initiative.getInitiativeId()));

        try {
            DroolsRule out = new DroolsRule();
            out.setId(initiative.getInitiativeId());
            out.setName(String.format("%s-%s", initiative.getInitiativeId(), initiative.getInitiativeName()));

            out.setRule("""
                    package it.gov.pagopa.admissibility.drools.buildrules;
                    
                    %s
                    """.formatted(
                    initiative.getBeneficiaryRule().getAutomatedCriteria().stream().map(c -> automatedCriteriaRuleBuild(out.getId(), out.getName(), c)).collect(Collectors.joining("\n\n")))
            );

            if(onlineSyntaxCheck){
                log.debug("Checking if the rule has valid syntax. id: %s".formatted(initiative.getInitiativeId()));
                builderService.build(Flux.just(out)).block(); // TODO handle if it goes to exception due to error
            }

            log.info("Conversion into drools rule completed; storing it. id: %s".formatted(initiative.getInitiativeId()));
            return out;
        } catch (RuntimeException e){
            log.error("Something gone wrong while building initiative %s".formatted(initiative.getInitiativeId()), e);
            return null;
        }
    }

    private String automatedCriteriaRuleBuild(String initiativeId, String ruleName, AutomatedCriteriaDTO automatedCriteriaDTO){
        CriteriaCodeConfig criteriaCodeConfig = criteriaCodeService.getCriteriaCodeConfig(automatedCriteriaDTO.getCode());
        if(criteriaCodeConfig == null){
            throw new IllegalStateException("Invalid criteria code provided or not configured: %s".formatted(automatedCriteriaDTO.getCode()));
        }
        return """
                rule "%s"
                agenda-group "%s"
                when $onboarding: %s(%s)
                then $onboarding.getOnboardingRejectionReasons().add("AUTOMATED_CRITERIA_%s_FAIL");
                end
                """.formatted(
                        ruleName + "-" + automatedCriteriaDTO.getCode(),
                        initiativeId,
                        OnboardingDroolsDTO.class.getName(),
                        extraFilter2DroolsTransformer.apply(automatedCriteria2ExtraFilter(automatedCriteriaDTO, criteriaCodeConfig), OnboardingDTO.class, null),
                automatedCriteriaDTO.getCode()
        );
    }

    private ExtraFilter automatedCriteria2ExtraFilter(AutomatedCriteriaDTO automatedCriteriaDTO, CriteriaCodeConfig criteriaCodeConfig) {
        String field = String.format("%s%s", criteriaCodeConfig.getOnboardingField(), StringUtils.isEmpty(automatedCriteriaDTO.getField()) ? "" : ".%s".formatted(automatedCriteriaDTO.getField()));
        FilterOperator operator = automatedCriteriaDTO.getOperator();
        return new NotOperation(new Filter(field, operator, automatedCriteriaDTO.getValue()));
    }
}
