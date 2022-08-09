package it.gov.pagopa.admissibility.mapper;


import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDTO;
import it.gov.pagopa.admissibility.dto.onboarding.OnboardingDroolsDTO;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class Onboarding2OnboardingDroolsMapper implements Function<OnboardingDTO, OnboardingDroolsDTO> {


    @Override
    public OnboardingDroolsDTO apply(OnboardingDTO onboardingDTO) {
        OnboardingDroolsDTO out = new OnboardingDroolsDTO();
        out.setUserId(onboardingDTO.getUserId());
        out.setInitiativeId(onboardingDTO.getInitiativeId());
        out.setTc(onboardingDTO.isTc());
        out.setStatus(onboardingDTO.getStatus());
        out.setPdndAccept(onboardingDTO.getPdndAccept());
        out.setSelfDeclarationList(onboardingDTO.getSelfDeclarationList());
        out.setTcAcceptTimestamp(onboardingDTO.getTcAcceptTimestamp());
        out.setCriteriaConsensusTimestamp(onboardingDTO.getCriteriaConsensusTimestamp());

        out.setIsee(onboardingDTO.getIsee());
        out.setResidenza(onboardingDTO.getResidenza());
        out.setBirthDate(onboardingDTO.getBirthDate());
        return out;
    }
}
