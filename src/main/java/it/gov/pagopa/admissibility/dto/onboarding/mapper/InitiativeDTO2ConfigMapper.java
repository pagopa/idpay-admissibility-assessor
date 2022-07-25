package it.gov.pagopa.admissibility.dto.onboarding.mapper;

import it.gov.pagopa.admissibility.dto.rule.beneficiary.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeConfig;
import it.gov.pagopa.admissibility.rest.initiative.dto.InitiativeDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service
public class InitiativeDTO2ConfigMapper implements Function<InitiativeDTO, InitiativeConfig> {
    @Override
    public InitiativeConfig apply(InitiativeDTO initiativeDTO) {
        InitiativeConfig out = new InitiativeConfig();
        out.setInitiativeId(initiativeDTO.getInitiativeId());
        out.setStartDate(initiativeDTO.getGeneral().getStartDate());
        out.setEndDate(initiativeDTO.getGeneral().getEndDate());
        out.setPdndToken(initiativeDTO.getPdndToken());
        List<String> automatedCriteriaCodes = getAutomatedCriteriaCodes(initiativeDTO.getBeneficiaryRule().getAutomatedCriteria());
        out.setAutomatedCriteriaCodes(automatedCriteriaCodes);
        out.setInitiativeBudget(initiativeDTO.getGeneral().getBudget());
        out.setBeneficiaryInitiativeBudget(initiativeDTO.getGeneral().getBeneficiaryBudget());
        out.setStatus(initiativeDTO.getStatus());
        return out;
    }

    private List<String> getAutomatedCriteriaCodes(List<AutomatedCriteriaDTO> initiativeAutomatedCriteria) {
        List<String> automatedCriteriaCodes = new ArrayList<>();
        for(AutomatedCriteriaDTO criterium : initiativeAutomatedCriteria) {
            automatedCriteriaCodes.add(criterium.getCode());
        }
        return automatedCriteriaCodes;
    }
}
