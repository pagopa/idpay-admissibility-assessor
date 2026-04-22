package it.gov.pagopa.admissibility.mapper;

import it.gov.pagopa.admissibility.dto.rule.*;
import it.gov.pagopa.admissibility.model.InitiativeConfig;
import it.gov.pagopa.admissibility.model.Order;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Service
public class Initiative2InitiativeConfigMapper implements Function<Initiative2BuildDTO, InitiativeConfig> {

    @Override
    public InitiativeConfig apply(Initiative2BuildDTO initiative) {

        List<AutomatedCriteriaDTO> automatedCriteriaList =
                initiative.getBeneficiaryRule().getAutomatedCriteria();

        InitiativeAdditionalInfoDTO additionalInfo = initiative.getAdditionalInfo();

        Long beneficiaryBudgetMaxCents =
                initiative.getGeneral().getBeneficiaryBudgetFixedCents() != null
                        ? null
                        : initiative.getBeneficiaryRule().getSelfDeclarationCriteria().stream()
                        .filter(SelfCriteriaMultiConsentDTO.class::isInstance)
                        .map(SelfCriteriaMultiConsentDTO.class::cast)
                        .flatMap(s -> s.getValue().stream())
                        .map(SelfCriteriaMultiConsentDTO.ConsentValue::getBeneficiaryBudgetMaxCents)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);

        return InitiativeConfig.builder()
                .initiativeId(initiative.getInitiativeId())
                .initiativeName(initiative.getInitiativeName())
                .organizationId(initiative.getOrganizationId())
                .organizationName(initiative.getOrganizationName())
                .status(initiative.getStatus())
                .automatedCriteria(automatedCriteriaList)
                .automatedCriteriaCodes(
                        automatedCriteriaList != null
                                ? automatedCriteriaList.stream()
                                .map(AutomatedCriteriaDTO::getCode)
                                .toList()
                                : null
                )
                .initiativeBudgetCents(initiative.getGeneral().getBudgetCents())
                .beneficiaryBudgetFixedCents(
                        initiative.getGeneral().getBeneficiaryBudgetFixedCents()
                )

                .beneficiaryBudgetMaxCents(beneficiaryBudgetMaxCents)

                .startDate(ObjectUtils.firstNonNull(
                        initiative.getGeneral().getRankingStartDate(),
                        initiative.getGeneral().getStartDate()
                ))
                .endDate(ObjectUtils.firstNonNull(
                        initiative.getGeneral().getRankingEndDate(),
                        initiative.getGeneral().getEndDate()
                ))
                .rankingInitiative(initiative.getGeneral().isRankingEnabled())
                .rankingFields(
                        Boolean.TRUE.equals(initiative.getGeneral().isRankingEnabled())
                                ? retrieveRankingFieldCodes(automatedCriteriaList)
                                : null
                )
                .initiativeRewardType(initiative.getInitiativeRewardType())
                .isLogoPresent(
                        additionalInfo != null &&
                                !StringUtils.isEmpty(additionalInfo.getLogoFileName())
                )
                .beneficiaryType(initiative.getGeneral().getBeneficiaryType())
                .build();
    }

    private List<Order> retrieveRankingFieldCodes(List<AutomatedCriteriaDTO> automatedCriteriaList) {
        return automatedCriteriaList != null ? automatedCriteriaList
                    .stream().filter(item -> item.getOrderDirection()!= null)
                    .map(automatedCriteria-> Order.builder()
                            .fieldCode(automatedCriteria.getCode())
                            .direction(automatedCriteria.getOrderDirection())
                            .build())
                    .toList()
                : Collections.emptyList();
    }
}
