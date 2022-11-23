package it.gov.pagopa.admissibility.service.build;

import it.gov.pagopa.admissibility.drools.transformer.extra_filter.ExtraFilter2DroolsTransformerFacade;
import it.gov.pagopa.admissibility.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingRejectionReason;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.mapper.AutomatedCriteria2ExtraFilterMapper;
import it.gov.pagopa.admissibility.mapper.Initiative2InitiativeConfigMapper;
import it.gov.pagopa.admissibility.model.CriteriaCodeConfig;
import it.gov.pagopa.admissibility.model.DroolsRule;
import it.gov.pagopa.admissibility.service.CriteriaCodeService;
import it.gov.pagopa.admissibility.utils.OnboardingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.stream.Collectors;

@Service
@Slf4j
public class BeneficiaryRule2DroolsRuleImpl implements BeneficiaryRule2DroolsRule {

    private final boolean onlineSyntaxCheck;

    private final Initiative2InitiativeConfigMapper initiative2InitiativeConfigMapper;
    private final CriteriaCodeService criteriaCodeService;
    private final AutomatedCriteria2ExtraFilterMapper automatedCriteria2ExtraFilterMapper;
    private final ExtraFilter2DroolsTransformerFacade extraFilter2DroolsTransformerFacade;
    private final KieContainerBuilderService builderService;

    public BeneficiaryRule2DroolsRuleImpl(@Value("${app.beneficiary-rule.online-syntax-check}") boolean onlineSyntaxCheck, Initiative2InitiativeConfigMapper initiative2InitiativeConfigMapper, CriteriaCodeService criteriaCodeService, AutomatedCriteria2ExtraFilterMapper automatedCriteria2ExtraFilterMapper, ExtraFilter2DroolsTransformerFacade extraFilter2DroolsTransformerFacade, KieContainerBuilderService builderService) {
        this.onlineSyntaxCheck = onlineSyntaxCheck;
        this.initiative2InitiativeConfigMapper = initiative2InitiativeConfigMapper;
        this.criteriaCodeService = criteriaCodeService;
        this.automatedCriteria2ExtraFilterMapper = automatedCriteria2ExtraFilterMapper;
        this.extraFilter2DroolsTransformerFacade = extraFilter2DroolsTransformerFacade;
        this.builderService = builderService;
    }

    @Override
    public DroolsRule apply(Initiative2BuildDTO initiative) {
        log.info("[BENEFICIARY_RULE_BUILDER] Building inititative having id: {}", initiative.getInitiativeId());

        DroolsRule out = new DroolsRule();
        out.setId(initiative.getInitiativeId());
        out.setName(String.format("%s-%s", initiative.getInitiativeId(), initiative.getInitiativeName()));

        out.setRule("""
                package %s;
                                    
                %s
                """.formatted(
                KieContainerBuilderServiceImpl.RULES_BUILT_PACKAGE,
                initiative.getBeneficiaryRule().getAutomatedCriteria().stream().map(c -> automatedCriteriaRuleBuild(out.getId(), out.getName(), c)).collect(Collectors.joining("\n\n")))
        );

        out.setInitiativeConfig(initiative2InitiativeConfigMapper.apply(initiative));

        if (onlineSyntaxCheck) {
            log.debug("[BENEFICIARY_RULE_BUILDER] Checking if the rule has valid syntax. id: {}", initiative.getInitiativeId());
            builderService.build(Flux.just(out)).block();
        }

        log.debug("[BENEFICIARY_RULE_BUILDER] Conversion into drools rule completed; storing it. id: {}", initiative.getInitiativeId());
        return out;
    }

    private String automatedCriteriaRuleBuild(String initiativeId, String ruleName, AutomatedCriteriaDTO automatedCriteriaDTO) {
        CriteriaCodeConfig criteriaCodeConfig = criteriaCodeService.getCriteriaCodeConfig(automatedCriteriaDTO.getCode());
        if (criteriaCodeConfig == null) {
            throw new IllegalStateException("[BENEFICIARY_RULE_BUILDER] Invalid criteria code provided or not configured: %s".formatted(automatedCriteriaDTO.getCode()));
        }
        return """
                rule "%s"
                agenda-group "%s"
                when
                   $criteriaCodeService: %s()
                   $onboarding: %s(%s)
                then
                   %s criteriaCodeConfig = $criteriaCodeService.getCriteriaCodeConfig("%s");
                   $onboarding.getOnboardingRejectionReasons().add(%s.builder().type(%s).code("%s").authority(criteriaCodeConfig.getAuthority()).authorityLabel(criteriaCodeConfig.getAuthorityLabel()).build());
                end
                """.formatted(
                ruleName + "-" + automatedCriteriaDTO.getCode(),
                initiativeId,
                CriteriaCodeService.class.getName(),
                OnboardingDroolsDTO.class.getName(),
                extraFilter2DroolsTransformerFacade.apply(
                        automatedCriteria2ExtraFilterMapper.apply(automatedCriteriaDTO, criteriaCodeConfig)
                        , OnboardingDTO.class, null),
                CriteriaCodeConfig.class.getName(),
                automatedCriteriaDTO.getCode(),
                OnboardingRejectionReason.class.getName(),
                DroolsTemplateRuleUtils.toTemplateParam(OnboardingRejectionReason.OnboardingRejectionReasonType.AUTOMATED_CRITERIA_FAIL).getParam().replace("$","."),
                OnboardingConstants.REJECTION_REASON_AUTOMATED_CRITERIA_FAIL_FORMAT.formatted(automatedCriteriaDTO.getCode())
        );
    }

}
