package it.gov.pagopa.admissibility.service.onboarding.check;

import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Order(0)
public class OnboardingConsensusCheck implements OnboardingCheck {
    @Override
    public String apply(OnboardingDTO onboardingDTO, Map<String, Object> onboardingContext) {
        if(!onboardingDTO.isTc()){
            return  "CONSENSUS_CHECK_TC_FAIL";
        }

        if(!onboardingDTO.getPdndAccept()){
            return "CONSENSUS_CHECK_PDND_FAIL";
        }

        if(onboardingDTO.getSelfDeclarationList().size() != 0 || onboardingDTO.getSelfDeclarationList() != null){
            String declarations = selfDeclarationListCheck(onboardingDTO.getSelfDeclarationList());
            if(declarations != null){
                return declarations;
            }
        }
        return null;
    }

    private String selfDeclarationListCheck(Map<String, Boolean> selfDeclarationList) {
        for (Map.Entry<String, Boolean> selfDeclaration : selfDeclarationList.entrySet()){
            if(Boolean.FALSE.equals(selfDeclaration.getValue())){
                return String.format("CONSENSUS_CHECK_SELF_DECLARATION_%s_FAIL",selfDeclaration.getKey());
            }
        }
        return null;
    }

}
