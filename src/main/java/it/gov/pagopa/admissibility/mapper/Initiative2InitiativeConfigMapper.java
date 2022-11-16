package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.rule.AnyOfInitiativeBeneficiaryRuleDTOSelfDeclarationCriteriaItems;
import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.dto.rule.SelfCriteriaBoolDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

@Service
public class Initiative2InitiativeConfigMapper implements Function<Initiative2BuildDTO, InitiativeConfig> {
    @Override
    public InitiativeConfig apply(Initiative2BuildDTO initiative) {
        InitiativeConfig out = InitiativeConfig.builder()
                .initiativeId(initiative.getInitiativeId())
                .initiativeName(initiative.getInitiativeName())
                .organizationId(initiative.getOrganizationId())
                .status(initiative.getStatus())
                .pdndToken(initiative.getPdndToken())
                .automatedCriteriaCodes(initiative.getBeneficiaryRule().getAutomatedCriteria().stream().map(AutomatedCriteriaDTO::getCode).toList())
                .initiativeBudget(initiative.getGeneral().getBudget())
                .beneficiaryInitiativeBudget(initiative.getGeneral().getBeneficiaryBudget())
                .startDate(ObjectUtils.firstNonNull(initiative.getGeneral().getRankingStartDate(), initiative.getGeneral().getStartDate()))
                .endDate(ObjectUtils.firstNonNull(initiative.getGeneral().getRankingEndDate(), initiative.getGeneral().getEndDate()))
                .serviceId(initiative.getAdditionalInfo() != null ? initiative.getAdditionalInfo().getServiceId() : null)
                .rankingInitiative(initiative.getRankingInitiative())
                .build();

        if(Boolean.TRUE.equals(initiative.getRankingInitiative())){
            setRankingFieldCodes(out,initiative);
        }

        return out;
    }

    private void setRankingFieldCodes(InitiativeConfig out, Initiative2BuildDTO initiative) {
        List<AnyOfInitiativeBeneficiaryRuleDTOSelfDeclarationCriteriaItems> selfDeclarationCriteria = initiative.getBeneficiaryRule()
                .getSelfDeclarationCriteria();

        if(selfDeclarationCriteria != null) {
            List<String> codeTrue = selfDeclarationCriteria.stream()
                    .filter(item -> item instanceof SelfCriteriaBoolDTO selfCriteriaBoolDTO && Boolean.TRUE.equals(selfCriteriaBoolDTO.getValue()))
                    .map(selfCriteria -> ((SelfCriteriaBoolDTO) selfCriteria).getCode())
                    .toList();

            out.setRankingFieldCodes(initiative.getBeneficiaryRule().getAutomatedCriteria()
                    .stream().map(AutomatedCriteriaDTO::getCode)
                    .filter(codeTrue::contains)
                    .toList()
            );
        }
    }
}
