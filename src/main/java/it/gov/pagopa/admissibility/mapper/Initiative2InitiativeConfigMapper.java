package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.rule.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.Initiative2BuildDTO;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Service
public class Initiative2InitiativeConfigMapper implements Function<Initiative2BuildDTO, InitiativeConfig> {
    @Override
    public InitiativeConfig apply(Initiative2BuildDTO initiative) {
        List<AutomatedCriteriaDTO> automatedCriteriaList = initiative.getBeneficiaryRule().getAutomatedCriteria();
        return InitiativeConfig.builder()
                .initiativeId(initiative.getInitiativeId())
                .initiativeName(initiative.getInitiativeName())
                .organizationId(initiative.getOrganizationId())
                .status(initiative.getStatus())
                .pdndToken(initiative.getPdndToken())
                .automatedCriteriaCodes(automatedCriteriaList != null ? automatedCriteriaList.stream().map(AutomatedCriteriaDTO::getCode).toList() : null)
                .initiativeBudget(initiative.getGeneral().getBudget())
                .beneficiaryInitiativeBudget(initiative.getGeneral().getBeneficiaryBudget())
                .startDate(ObjectUtils.firstNonNull(initiative.getGeneral().getRankingStartDate(), initiative.getGeneral().getStartDate()))
                .endDate(ObjectUtils.firstNonNull(initiative.getGeneral().getRankingEndDate(), initiative.getGeneral().getEndDate()))
                .serviceId(initiative.getAdditionalInfo() != null ? initiative.getAdditionalInfo().getServiceId() : null)
                .rankingInitiative(initiative.getRankingInitiative())
                .rankingFieldCodes(Boolean.TRUE.equals(initiative.getRankingInitiative()) ? retrieveRankingFieldCodes(automatedCriteriaList) : null)
                .build();
    }

    private List<String> retrieveRankingFieldCodes(List<AutomatedCriteriaDTO> automatedCriteriaList) {
        return automatedCriteriaList != null ? automatedCriteriaList
                    .stream().filter(item -> Boolean.TRUE.equals(item.getOrderEnabled()))
                    .map(AutomatedCriteriaDTO::getCode)
                    .toList()
                : Collections.emptyList();
    }
}
