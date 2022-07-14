package it.gov.pagopa.admissibility.rest.initiative;

import it.gov.pagopa.admissibility.drools.model.filter.FilterOperator;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.AutomatedCriteriaDTO;
import it.gov.pagopa.admissibility.dto.rule.beneficiary.InitiativeBeneficiaryRuleDTO;
import it.gov.pagopa.admissibility.rest.initiative.dto.InitiativeDTO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Service
public class InitiativeRestServiceImpl implements InitiativeRestService{
    @Override
    public Mono<InitiativeDTO> findById(String initiativeId) { // TODO
        AutomatedCriteriaDTO mockedCriteria = new AutomatedCriteriaDTO();
        mockedCriteria.setAuthority("AUTHORITY1");
        mockedCriteria.setCode("CRITERIACODE1");
        mockedCriteria.setOperator(FilterOperator.EQ);
        mockedCriteria.setValue("10");

        InitiativeDTO mockedResponse = new InitiativeDTO();
        mockedResponse.setInitiativeId(initiativeId);
        mockedResponse.setBeneficiaryRule(new InitiativeBeneficiaryRuleDTO());
        mockedResponse.getBeneficiaryRule().setAutomatedCriteria(Collections.singletonList(mockedCriteria));

        return Mono.just(mockedResponse);
    }
}
