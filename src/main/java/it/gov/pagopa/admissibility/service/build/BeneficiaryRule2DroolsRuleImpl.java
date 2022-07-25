package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.drools.model.ExtraFilter;
import it.gov.pagopa.admissibility.drools.model.NotOperation;
import it.gov.pagopa.admissibility.drools.model.filter.Filter;
import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformerFacade;
import it.gov.pagopa.admissibility.dto.build.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BeneficiaryRule2DroolsRuleImpl implements BeneficiaryRule2DroolsRule {

    private final boolean onlineSyntaxCheck;

    private final CriteriaCodeService criteriaCodeService;
    private final ExtraFilter2DroolsTransformerFacade extraFilter2DroolsTransformerFacade;
    private final KieContainerBuilderService builderService;

    private static final String START = "START";
    private static final String END = "END";

    public BeneficiaryRule2DroolsRuleImpl(@Value("${app.beneficiary-rule.online-syntax-check}") boolean onlineSyntaxCheck, CriteriaCodeService criteriaCodeService, ExtraFilter2DroolsTransformerFacade extraFilter2DroolsTransformerFacade, KieContainerBuilderService builderService) {
        this.onlineSyntaxCheck = onlineSyntaxCheck;
        this.criteriaCodeService = criteriaCodeService;
        this.extraFilter2DroolsTransformerFacade = extraFilter2DroolsTransformerFacade;
        this.builderService = builderService;
    }

    @Override
    public DroolsRule apply(Initiative2BuildDTO initiative) {
        log.info("Building inititative having id: %s".formatted(initiative.getInitiativeId()));

        try {
            DroolsRule out = new DroolsRule();
            out.setId(initiative.getInitiativeId());
            out.setName(String.format("%s-%s", initiative.getInitiativeId(), initiative.getInitiativeName()));

            out.setRule("""
                    package %s;
                                        
                    %s
                    """.formatted(
                    KieContainerBuilderServiceImpl.rulesBuiltPackage,
                    initiative.getBeneficiaryRule().getAutomatedCriteria().stream().map(c -> automatedCriteriaRuleBuild(out.getId(), out.getName(), c)).collect(Collectors.joining("\n\n")))
            );

            Map<String, LocalDate> startAndEndDates = setStartAndEndDate(initiative);

            InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                    .initiativeId(initiative.getInitiativeId())
                    .pdndToken(initiative.getPdndToken())
                    .automatedCriteriaCodes(initiative.getBeneficiaryRule().getAutomatedCriteria().stream().map(AutomatedCriteriaDTO::getCode).toList())
                    .initiativeBudget(initiative.getGeneral().getBudget())
                    .beneficiaryInitiativeBudget(initiative.getGeneral().getBeneficiaryBudget())
                    .startDate(startAndEndDates.get(START))
                    .endDate(startAndEndDates.get(END))
                    .build();

            out.setInitiativeConfig(initiativeConfig);

            if(onlineSyntaxCheck){
                log.debug("Checking if the rule has valid syntax. id: %s".formatted(initiative.getInitiativeId()));
                builderService.build(Flux.just(out)).block(); // TODO handle if it goes to exception due to error
            }

            log.debug("Conversion into drools rule completed; storing it. id: %s".formatted(initiative.getInitiativeId()));
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
                        extraFilter2DroolsTransformerFacade.apply(automatedCriteria2ExtraFilter(automatedCriteriaDTO, criteriaCodeConfig), OnboardingDTO.class, null),
                automatedCriteriaDTO.getCode()
        );
    }

    private ExtraFilter automatedCriteria2ExtraFilter(AutomatedCriteriaDTO automatedCriteriaDTO, CriteriaCodeConfig criteriaCodeConfig) {
        String field = String.format("%s%s", criteriaCodeConfig.getOnboardingField(), StringUtils.isEmpty(automatedCriteriaDTO.getField()) ? "" : ".%s".formatted(automatedCriteriaDTO.getField()));
        FilterOperator operator = automatedCriteriaDTO.getOperator();
        return new NotOperation(new Filter(field, operator, automatedCriteriaDTO.getValue()));
    }

    private Map<String, LocalDate> setStartAndEndDate(Initiative2BuildDTO initiative){
        Map<String, LocalDate> startAndEndDates = new HashMap<>();
        if(initiative.getGeneral().getRankingStartDate()==null || initiative.getGeneral().getRankingEndDate()==null) {
            startAndEndDates.put(START,initiative.getGeneral().getStartDate());
            startAndEndDates.put(END,initiative.getGeneral().getEndDate());
        }else{
            startAndEndDates.put(START,initiative.getGeneral().getRankingStartDate());
            startAndEndDates.put(END,initiative.getGeneral().getRankingEndDate());
        }
        return startAndEndDates;
    }
}
